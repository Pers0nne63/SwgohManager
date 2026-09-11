package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.Joueur;
import swgohManager.model.SyncExecution;
import swgohManager.repository.GuildBilanActuelRepository;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.SyncExecutionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildFullSyncService {

    private final GuildSyncService guildSyncService;
    private final JoueurRepository joueurRepository;
    private final PlayerSyncService playerSyncService;
    private final SyncExecutionRepository syncExecutionRepository;
    private final SyncProgressService progressService;
    private final GuildBilanService guildBilanService;
    private final GuildBilanActuelRepository guildBilanActuelRepository;
    private final GuildSyncReferentialService guildSyncReferentialService;

    // Services à nettoyer après la synchronisation
    private final RosterUnitService rosterUnitService;
    private final FarmPlanProgressService farmPlanProgressService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final PlayerModQService playerModQService;
    private final FarmPlanIndProgressService farmPlanIndProgressService;
    private final PlayerDatacronService playerDatacronService;
    private final PlayerEraUnitStatusService playerEraUnitStatusService;

    @Qualifier("playerSyncExecutor")
    private final ExecutorService playerSyncExecutor;
    
    @Value("${swgoh.guild.id}")
    private String guildId;

    public GuildFullSyncResult synchroniserGuildeComplete(boolean withProgress) {
        GuildSyncService.GuildSyncResult guildResult = guildSyncService.synchroniserGuilde();

        // Un seul idSync pour l'ensemble des joueurs de ce lot
        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();

        log.info("Chargement du référentiel commun pour la synchro de guilde...");
        long t0 = System.currentTimeMillis();
        GuildSyncReferentialCache cache = guildSyncReferentialService.charger();
        log.info("Référentiel commun chargé en {} ms", System.currentTimeMillis() - t0);

        List<Joueur> joueursPresents = joueurRepository.findAllByPresentInGuildTrue();
        int totalJoueurs = joueursPresents.size();
        log.info("Synchronisation de {} joueur(s) sous idSync={}, 10 en parallèle", joueursPresents.size(), idSync);

        AtomicInteger jouersTraites = new AtomicInteger(0);
        
        List<CompletableFuture<SyncOutcome>> futures = joueursPresents.stream()
        		.map(joueur -> CompletableFuture.supplyAsync(
                        () -> synchroniserUnJoueur(joueur, idSync, cache), playerSyncExecutor)
                        .thenApply(outcome -> {
                            int traites = jouersTraites.incrementAndGet();
                            if (withProgress && totalJoueurs > 0) {
                                // Progression jusqu'à 95% vu que STATQ et Objectifs sont dedans
                                int percent = (int) ((traites / (double) totalJoueurs) * 95);
                                String step = String.format("Joueurs (%d/%d)", traites, totalJoueurs);
                                String msg = String.format("Joueur %s synchronisé (Stats & STATQ inclus)", joueur.getPlayerName());
                                progressService.notifyProgress("guild", percent, step, msg);
                            }
                            return outcome;
                        }))
                .toList();

        List<String> succes = new ArrayList<>();
        List<String> echecs = new ArrayList<>();

        for (CompletableFuture<SyncOutcome> future : futures) {
            SyncOutcome outcome = future.join();
            if (outcome.succes()) {
                succes.add(outcome.playerId());
            } else {
                echecs.add(outcome.playerId());
            }
        }

        // Nettoyage global des données des anciens joueurs
        log.info("Lancement du nettoyage des tables '_actuel' pour les joueurs inactifs...");
        if (withProgress) progressService.notifyProgress("guild", 96, "Nettoyage", "Nettoyage des joueurs inactifs...");
        
        try {
            List<String> joueursActifs = joueursPresents.stream()
                    .map(Joueur::getPlayerId)
                    .toList();

            if (!joueursActifs.isEmpty()) {
                farmPlanIndProgressService.nettoyerJoueursInactifs(joueursActifs);
                farmPlanProgressService.nettoyerJoueursInactifs(joueursActifs);
                omicronPlanProgressService.nettoyerJoueursInactifs(joueursActifs);
                playerModQService.nettoyerJoueursInactifs(joueursActifs);
                rosterUnitService.nettoyerJoueursInactifs(joueursActifs);
                playerDatacronService.nettoyerJoueursInactifs(joueursActifs);
                playerEraUnitStatusService.nettoyerJoueursInactifs(joueursActifs);
                
                log.info("Nettoyage des joueurs inactifs terminé avec succès.");
            } else {
                log.warn("Aucun joueur actif trouvé, nettoyage annulé par sécurité.");
            }
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des joueurs inactifs : {}", e.getMessage(), e);
        }
        
        try {
            if (withProgress) progressService.notifyProgress("guild", 98, "Bilan de guilde", "Rafraîchissement du bilan global...");
        	guildBilanActuelRepository.deleteByGuildId(guildId);
        	guildBilanActuelRepository.flush();
        	guildBilanService.rafraichir();
        } catch (Exception e) {
            log.error("Échec du rafraîchissement du bilan de guilde : {}", e.getMessage(), e);
        }

        String resume = String.format("%d joueur(s) synchronisé(s) avec succès, %d échec(s) (idSync=%d)",
                succes.size(), echecs.size(), idSync);
        log.info(resume);

        return new GuildFullSyncResult(guildResult, succes.size(), echecs, resume);
    }

    private SyncOutcome synchroniserUnJoueur(Joueur joueur, Long idSync, GuildSyncReferentialCache cache) {
        try {
            PlayerIdentifier identifier = PlayerIdentifier.of(joueur.getPlayerId(), null);
            playerSyncService.synchroniserJoueur(identifier, idSync, cache);
            return new SyncOutcome(joueur.getPlayerId(), true);
        } catch (Exception e) {
            log.error("Échec de synchronisation du joueur {} ({}) : {}",
                    joueur.getPlayerName(), joueur.getPlayerId(), e.getMessage());
            return new SyncOutcome(joueur.getPlayerId(), false);
        }
    }

    private record SyncOutcome(String playerId, boolean succes) {}

    public record GuildFullSyncResult(
            GuildSyncService.GuildSyncResult guildSync,
            int joueursSynchronises,
            List<String> joueursEnEchec,
            String resume
    ) {}
}