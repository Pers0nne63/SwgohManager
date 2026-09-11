package swgohManager.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.GuildResponse;
import swgohManager.model.GuildBilanActuel;
import swgohManager.model.GuildComparisonResult;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.repository.ExternalPlayerStatqActuelRepository;
import swgohManager.repository.ExternalPlayerStatqDetailActuelRepository;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.GuildComparisonResultRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalGuildScanService {

    private static final String SYNC_TYPE = "guildComparison";
    private static final String MODE_TB = "TERRITORY_BATTLE_BOTH_OMICRON";
    private static final String MODE_TW = "TERRITORY_WAR_OMICRON";

    // Thread dédié uniquement à l'orchestration : ne consomme pas le pool playerSyncExecutor
    private final ExecutorService guildComparisonExecutor = Executors.newSingleThreadExecutor();

    private final SwgohApiClient swgohApiClient;
    private final PlayerSyncService playerSyncService;
    private final GuildBilanService guildBilanService;
    private final SyncProgressService progressService;

    private final ExternalPlayerRepository externalPlayerRepository;
    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ExternalPlayerStatqActuelRepository externalPlayerStatqActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalPlayerStatqDetailActuelRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalPlayerDatacronActuelRepository externalPlayerDatacronActuelRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalPlayerDatacronAffixActuelRepository;
    private final ExternalRosterUnitSkillActuelRepository externalRosterUnitSkillActuelRepository;

    private final GuildComparisonResultRepository guildComparisonResultRepository;
    private final ObjectMapper objectMapper;

    @Qualifier("playerSyncExecutor")
    private final ExecutorService playerSyncExecutor;

    /** Point d'entrée depuis le controller : renvoie immédiatement, le scan tourne en tâche de fond. */
    public void lancerComparaisonAsync(String guildId) {
        guildComparisonExecutor.submit(() -> {
            try {
                comparer(guildId);
            } catch (Exception e) {
                log.error("Échec de la comparaison pour guildId {} : {}", guildId, e.getMessage(), e);
                progressService.notifyError(SYNC_TYPE, "Impossible de comparer cette guilde : " + e.getMessage());
            }
        });
    }

    @Transactional
    public GuildComparisonResult comparer(String guildId) {
        progressService.notifyProgress(SYNC_TYPE, 2, "Connexion", "Récupération des informations de guilde...");

        GuildResponse response = swgohApiClient.getGuild(guildId);
        GuildResponse.Guild guilde = response.guild();
        if (guilde == null || guilde.member() == null) {
            throw new IllegalStateException("Réponse /guild vide ou invalide pour guildId=" + guildId);
        }
        String guildNom = guilde.getGuildName();
        int totalMembres = guilde.member().size();

        log.info("Scan parallèle de la guilde externe {} ({}) — {} membre(s)", guildNom, guildId, totalMembres);

        AtomicInteger succes = new AtomicInteger(0);
        AtomicInteger echecs = new AtomicInteger(0);
        AtomicInteger traites = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = guilde.member().stream()
                .map(membre -> CompletableFuture.runAsync(() -> {
                    try {
                        playerSyncService.scannerExterneParPlayerId(membre.playerId());
                        succes.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Échec du scan du membre {} ({}) de la guilde {} : {}",
                                membre.playerName(), membre.playerId(), guildId, e.getMessage());
                        echecs.incrementAndGet();
                    } finally {
                        int fait = traites.incrementAndGet();
                        // Plage de 5% à 85% pour le scan des membres
                        int percent = 5 + (int) ((fait / (double) totalMembres) * 80);
                        progressService.notifyProgress(SYNC_TYPE, percent,
                                String.format("Membres (%d/%d)", fait, totalMembres),
                                "Scan de " + membre.playerName());
                    }
                }, playerSyncExecutor))
                .toList();

        // Attente de la fin de l'ensemble des tâches asynchrones
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Scan guilde {} terminé : {} succès, {} échec(s)", guildId, succes.get(), echecs.get());

        progressService.notifyProgress(SYNC_TYPE, 88, "Agrégation", "Calcul des indicateurs de comparaison...");
        GuildComparisonResult resultat = construireResultat(guildId, guildNom, guilde);

        guildComparisonResultRepository.findByGuildIdB(guildId)
                .ifPresent(existing -> {
                    guildComparisonResultRepository.delete(existing);
                    guildComparisonResultRepository.flush(); // Force l'exécution du DELETE en BDD immédiatement
                });

        GuildComparisonResult sauvegarde = guildComparisonResultRepository.save(resultat);

        progressService.notifyProgress(SYNC_TYPE, 95, "Nettoyage", "Purge des données temporaires...");
        // Purge des données externes une fois le résultat calculé et persisté
        playerSyncService.purgerParGuildId(guildId);

        progressService.notifyProgress(SYNC_TYPE, 100, "Terminé", "Comparaison disponible");
        return sauvegarde;
    }

    @Transactional
    protected GuildComparisonResult construireResultat(String guildId, String guildNom, GuildResponse.Guild guilde) {
        GuildBilanActuel bilanA = guildBilanService.getBilanActuelOuRafraichir();

        var modq = externalPlayerModQActuelRepository.findAggregatByGuildId(guildId);
        var relics = externalRosterUnitActuelRepository.findRepartitionRelicsByGuildId(guildId);
        var statqTeams = externalPlayerStatqDetailActuelRepository.findMoyenneNoteParTeamByGuildId(guildId);

        long omicronTb = 0, omicronTw = 0;
        for (var p : externalRosterUnitSkillActuelRepository.sommeParModeByGuildId(guildId, List.of(MODE_TB, MODE_TW))) {
            if (MODE_TB.equals(p.getOmicronMode())) omicronTb = p.getTotal();
            if (MODE_TW.equals(p.getOmicronMode())) omicronTw = p.getTotal();
        }

        Integer etoilesBT = extraireDernieresEtoiles(guilde);
        Long scoreRaidTotal = extraireScoreRaidTotal(guilde);

        return GuildComparisonResult.builder()
                .dateComparaison(Instant.now())
                .guildIdA(bilanA.getGuildId())
                .guildNomA(bilanA.getGuildNom())
                .nbMembresA(bilanA.getNbMembres())
                .galacticPowerTotalA(bilanA.getGalacticPowerTotal())
                .etoilesBtA(bilanA.getEtoilesBT())
                .scoreRaidTotalA(bilanA.getScoreRaidTotal())
                .modQMoyenA(bilanA.getModQMoyen())
                .mod25PlusA(bilanA.getMod25Plus())
                .mod20A24A(bilanA.getMod20A24())
                .mod15A19A(bilanA.getMod15A19())
                .statQMoyenA(bilanA.getStatQMoyen())
                .relic10A(bilanA.getRelic10())
                .relic9A(bilanA.getRelic9())
                .relic8A(bilanA.getRelic8())
                .relic6Et7A(bilanA.getRelic6Et7())
                .relic0A5A(bilanA.getRelic0A5())
                .sansRelicA(bilanA.getSansRelic())
                .nbFdtcA(bilanA.getNbFdtc())
                .nbDtc9A(bilanA.getNbDtc9())
                .nbOmicronTbA(bilanA.getNbOmicronTb())
                .nbOmicronTwA(bilanA.getNbOmicronTw())
                .statQParTeamJsonA(bilanA.getStatQParTeamJson())
                .guildIdB(guildId)
                .guildNomB(guildNom)
                .nbMembresB(guilde.member().size())
                .galacticPowerTotalB(externalPlayerRepository.sumGalacticPowerByGuildId(guildId))
                .etoilesBtB(etoilesBT)
                .scoreRaidTotalB(scoreRaidTotal)
                .modQMoyenB(modq.getModQMoyen())
                .mod25PlusB(modq.getMod25Plus())
                .mod20A24B(modq.getMod20A24())
                .mod15A19B(modq.getMod15A19())
                .statQMoyenB(externalPlayerStatqActuelRepository.findMoyenneStatqByGuildId(guildId))
                .relic10B(relics.getRelic10())
                .relic9B(relics.getRelic9())
                .relic8B(relics.getRelic8())
                .relic6Et7B(relics.getRelic6Et7())
                .relic0A5B(relics.getRelic0A5())
                .sansRelicB(relics.getSansRelic())
                .nbFdtcB(externalPlayerDatacronActuelRepository.countFdtcByGuildId(guildId))
                .nbDtc9B(externalPlayerDatacronAffixActuelRepository.countDtc9ByGuildId(guildId))
                .nbOmicronTbB(omicronTb)
                .nbOmicronTwB(omicronTw)
                .statQParTeamJsonB(serialiser(statqTeams))
                .build();
    }

    private Integer extraireDernieresEtoiles(GuildResponse.Guild guilde) {
        if (guilde.recentTerritoryBattleResult() == null || guilde.recentTerritoryBattleResult().isEmpty()) return null;
        return guilde.recentTerritoryBattleResult().stream()
                .max(Comparator.comparingLong(tb -> Long.parseLong(tb.endTime())))
                .map(tb -> tb.totalStars() != null ? Integer.parseInt(tb.totalStars()) : null)
                .orElse(null);
    }

    private Long extraireScoreRaidTotal(GuildResponse.Guild guilde) {
        if (guilde.recentRaidResult() == null || guilde.recentRaidResult().isEmpty()) return null;
        return guilde.recentRaidResult().stream()
                .max(Comparator.comparingLong(GuildResponse.RecentRaidResult::endTime))
                .map(raid -> raid.raidMember().stream().mapToLong(GuildResponse.RaidMember::memberProgress).sum())
                .orElse(null);
    }

    private String serialiser(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }
}