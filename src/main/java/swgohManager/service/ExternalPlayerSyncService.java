package swgohManager.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.ExternalPlayer;
import swgohManager.model.ExternalRosterUnitActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.ExternalRosterUnitModActuelRepository;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerSyncService {

    private final SwgohApiClient swgohApiClient;
    private final ExternalPlayerRepository externalPlayerRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalRosterUnitModActuelRepository externalRosterUnitModActuelRepository;
    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ExternalPlayerModQService externalPlayerModQService;
    private final ExternalGuildScanService externalGuildScanService;

    // Purge
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;
    private final ExternalRosterUnitSkillActuelRepository externalSkillRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;

    // Service dédié aux compétences et datacrons
    private final ExternalPlayerSyncDataService externalPlayerSyncDataService;

    @Transactional
    public String scanner(String allyCode) {
        PlayerIdentifier identifier = PlayerIdentifier.of(null, allyCode);
        PlayerResponse response = swgohApiClient.getPlayer(identifier);
        String playerId = response.playerId();

        // Nettoyage avant réinsertion
        externalRosterUnitActuelRepository.deleteByPlayerId(playerId);
        externalRosterUnitModActuelRepository.deleteByPlayerId(playerId);
        externalPlayerModQActuelRepository.deleteByPlayerId(playerId);
        externalPlayerRepository.deleteByPlayerId(playerId);

        externalRosterUnitActuelRepository.flush();
        externalRosterUnitModActuelRepository.flush();
        externalPlayerModQActuelRepository.flush();
        externalPlayerRepository.flush();

        Integer skillRating = response.playerRating() != null && response.playerRating().playerSkillRating() != null
                ? response.playerRating().playerSkillRating().skillRating() : null;
        String leagueId = response.playerRating() != null && response.playerRating().playerRankStatus() != null
                ? response.playerRating().playerRankStatus().leagueId() : null;
        Integer divisionId = response.playerRating() != null && response.playerRating().playerRankStatus() != null
                ? response.playerRating().playerRankStatus().divisionId() : null;

        ExternalPlayer externalPlayer = ExternalPlayer.builder()
                .playerId(playerId)
                .allyCode(response.allyCode())
                .playerName(response.name())
                .guildId(response.guildId())
                .guildName(response.guildName())
                .characterGalacticPower(null)
                .leagueId(leagueId)
                .skillRating(skillRating)
                .divisionId(divisionId)
                .dateScan(Instant.now())
                .build();
        externalPlayerRepository.save(externalPlayer);

        List<ExternalRosterUnitActuel> unites = new ArrayList<>();
        List<ExternalRosterUnitModActuel> mods = new ArrayList<>();

        if (response.rosterUnit() != null) {
            for (PlayerResponse.RosterUnit u : response.rosterUnit()) {
                Integer relicValue = u.relic() != null && u.relic().currentTier() != null
                        ? u.relic().currentTier() - 2 : null;

                unites.add(ExternalRosterUnitActuel.builder()
                        .playerId(playerId).idUnit(u.id()).definitionId(u.definitionId())
                        .etoiles(u.currentRarity()).niveau(u.currentLevel()).gear(u.currentTier())
                        .relic(relicValue)
                        .build());

                if (u.equippedStatMod() != null) {
                    for (PlayerResponse.EquippedStatMod mod : u.equippedStatMod()) {
                        mods.addAll(construireLignesMod(playerId, u.id(), mod));
                    }
                }
            }
        }

        // 1. Sauvegarde des unités et des mods
        externalRosterUnitActuelRepository.saveAll(unites);
        externalRosterUnitModActuelRepository.saveAll(mods);

        // 2. Sauvegarde des compétences (skills) et datacrons
        externalPlayerSyncDataService.enregistrerSkillsEtDatacrons(playerId, response);

        // 3. Scan de guilde pour récupérer le nom et les puissances galactiques
        ExternalGuildScanService.GuildPlayerData guildData = externalGuildScanService.scannerGuildeDuJoueur(playerId, response.guildId());
        if (guildData != null) {
            externalPlayer.setGuildName(guildData.guildName());
            externalPlayer.setGalacticPower(guildData.galacticPower());
            externalPlayer.setCharacterGalacticPower(guildData.characterGalacticPower());
            externalPlayer.setShipGalacticPower(guildData.shipGalacticPower());
            externalPlayerRepository.save(externalPlayer);
        }

        // 4. Calcul du ModQ une fois characterGalacticPower disponible
        externalPlayerModQService.calculerEtEnregistrer(
                playerId, 
                mods, 
                externalPlayer.getCharacterGalacticPower()
        );

        String message = String.format("Scan externe : %s (%s) — %d unité(s), %d ligne(s) de mod",
                response.name(), playerId, unites.size(), mods.size());
        log.info(message);
        return playerId;
    }

    private List<ExternalRosterUnitModActuel> construireLignesMod(String playerId, String idUnit, PlayerResponse.EquippedStatMod mod) {
        List<ExternalRosterUnitModActuel> lignes = new ArrayList<>();

        String definitionId = mod.definitionId();
        String set = definitionId != null && definitionId.length() >= 1 ? definitionId.substring(0, 1) : null;
        String rarity = definitionId != null && definitionId.length() >= 2 ? definitionId.substring(1, 2) : null;
        String position = definitionId != null && definitionId.length() >= 3 ? definitionId.substring(2, 3) : null;

        Integer idPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? mod.primaryStat().stat().unitStatId() : null;
        Long valeurPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? parseLong(mod.primaryStat().stat().unscaledDecimalValue()) : null;

        List<PlayerResponse.SecondaryStat> secondaires = mod.secondaryStat() != null ? mod.secondaryStat() : List.of();

        int ordre = 1;
        for (PlayerResponse.SecondaryStat secondaryStat : secondaires) {
            Integer idSecondaire = secondaryStat.stat() != null ? secondaryStat.stat().unitStatId() : null;
            Long valeurSecondaire = secondaryStat.stat() != null
                    ? parseLong(secondaryStat.stat().unscaledDecimalValue()) : null;

            lignes.add(ExternalRosterUnitModActuel.builder()
                    .playerId(playerId).idUnit(idUnit).idMod(mod.id()).definitionId(definitionId)
                    .set(set).rarity(rarity).position(position).niveau(mod.level())
                    .idPrimaire(idPrimaire).valeurPrimaire(valeurPrimaire)
                    .idSecondaire(idSecondaire).valeurSecondaire(valeurSecondaire)
                    .ordreSecondaire(ordre)
                    .build());
            ordre++;
        }
        return lignes;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }

    /** Purge des scans de plus de 30 jours (joueur + roster + mods + modQ + raid + TB + skills + datacrons). */
    @Transactional
    public int purgerAnciensScan() {
        Instant seuil = Instant.now().minus(30, ChronoUnit.DAYS);
        List<String> playerIdsAPurger = externalPlayerRepository.findPlayerIdsScannesAvant(seuil);

        for (String playerId : playerIdsAPurger) {
            externalRosterUnitActuelRepository.deleteByPlayerId(playerId);
            externalRosterUnitModActuelRepository.deleteByPlayerId(playerId);
            externalPlayerModQActuelRepository.deleteByPlayerId(playerId);
            externalPlayerRaidRepository.deleteByPlayerId(playerId);
            externalPlayerTbScoreRepository.deleteByPlayerId(playerId);
            
            // Nettoyage des compétences et datacrons lors de la purge
            externalSkillRepository.deleteByPlayerId(playerId);
            externalAffixRepository.deleteByPlayerId(playerId);
            externalDatacronRepository.deleteByPlayerId(playerId);

            externalPlayerRepository.deleteByPlayerId(playerId);
        }
        log.info("Purge des scans externes : {} joueur(s) supprimé(s)", playerIdsAPurger.size());
        return playerIdsAPurger.size();
    }
}