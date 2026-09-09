package swgohManager.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.UnitModDTO;
import swgohManager.model.ExternalPlayer;
import swgohManager.model.ExternalRosterUnitActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.ExternalPlayerEraUnitStatusActuelRepository;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerRatingActuelRepository;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.repository.ExternalPlayerStatqActuelRepository;
import swgohManager.repository.ExternalPlayerStatqDetailActuelRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.ExternalRosterUnitModActuelRepository;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.ExternalRosterUnitStatActuelRepository;
import swgohManager.repository.ExternalRosterUnitStatObjectifRepository;

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
    private final UnitModCalculationService unitModCalculationService;
    private final RosterUnitStatCalculService rosterUnitStatCalculService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;
    private final StatqCalculService statqCalculService;
    private final PlayerEraUnitStatusService playerEraUnitStatusService; 
    private final PlayerRatingService playerRatingService;
    private final ExternalPlayerRatingActuelRepository externalPlayerRatingActuelRepository;
    private final RosterUnitService rosterUnitService;
    private final PlayerDatacronService playerDatacronService;

    // Purge
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;
    private final ExternalRosterUnitSkillActuelRepository externalSkillRepository;
    private final ExternalRosterUnitStatActuelRepository externalStatActuelRepository;
    private final ExternalRosterUnitStatObjectifRepository externalStatObjectifRepository;
    private final ExternalPlayerStatqActuelRepository externalStatqActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalStatqDetailActuelRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;
    private final ExternalPlayerEraUnitStatusActuelRepository externalPlayerEraUnitStatusActuelRepository; 


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
        
        ExternalPlayer externalPlayer = ExternalPlayer.builder()
                .playerId(playerId)
                .allyCode(response.allyCode())
                .playerName(response.name())
                .guildId(response.guildId())
                .guildName(response.guildName())
                .characterGalacticPower(null)
                .dateScan(Instant.now())
                .build();
        externalPlayerRepository.save(externalPlayer);

        playerRatingService.enregistrerRating(response, Portee.EXTERNE);

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
                        List<UnitModDTO> lignesDto = unitModCalculationService.construireLignesModDto(u.id(), mod);
                        for (UnitModDTO dto : lignesDto) {
                            mods.add(ExternalRosterUnitModActuel.builder()
                                    .playerId(playerId)
                                    .idUnit(dto.idUnit())
                                    .idMod(dto.idMod())
                                    .definitionId(dto.definitionId())
                                    .set(dto.set())
                                    .rarity(dto.rarity())
                                    .position(dto.position())
                                    .niveau(dto.niveau())
                                    .idPrimaire(dto.idPrimaire())
                                    .valeurPrimaire(dto.valeurPrimaire())
                                    .idSecondaire(dto.idSecondaire())
                                    .valeurSecondaire(dto.valeurSecondaire())
                                    .ordreSecondaire(dto.ordreSecondaire())
                                    .build());
                        }
                    }
                }
            }
        }

        // 1. Sauvegarde des unités et des mods
        externalRosterUnitActuelRepository.saveAll(unites);
        externalRosterUnitModActuelRepository.saveAll(mods);

     // 2. Sauvegarde des compétences (skills) et datacrons
        Map<String, SkillDefinition> definitions = rosterUnitService.chargerDefinitionsSkill();
        rosterUnitService.enregistrerSkills(playerId, response.rosterUnit(), definitions, Portee.EXTERNE, null);
        playerDatacronService.enregistrer(playerId, response, Portee.EXTERNE);
        
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

        // 5. Calcul des stats actuelles, des objectifs, puis du StatQ
        rosterUnitStatCalculService.calculerEtEnregistrerExterne(playerId, unites, mods);
        rosterUnitStatObjectifService.calculerEtEnregistrerExterne(playerId, unites);
        statqCalculService.calculerEtEnregistrer(playerId);
        
        // 6. Unités d'ère
        playerEraUnitStatusService.enregistrer(playerId, response, Portee.EXTERNE);

        String message = String.format("Scan externe : %s (%s) — %d unité(s), %d ligne(s) de mod",
                response.name(), playerId, unites.size(), mods.size());
        log.info(message);
        return playerId;
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
            externalSkillRepository.deleteByPlayerId(playerId);
            externalAffixRepository.deleteByPlayerId(playerId);
            externalDatacronRepository.deleteByPlayerId(playerId);
            externalStatActuelRepository.deleteByPlayerId(playerId);
            externalStatObjectifRepository.deleteByPlayerId(playerId);
            externalStatqActuelRepository.deleteByPlayerId(playerId);
            externalStatqDetailActuelRepository.deleteByPlayerId(playerId);
            externalPlayerRepository.deleteByPlayerId(playerId);
            externalPlayerEraUnitStatusActuelRepository.deleteByPlayerId(playerId);
            externalPlayerRatingActuelRepository.deleteByPlayerId(playerId);
        }
        log.info("Purge des scans externes : {} joueur(s) supprimé(s)", playerIdsAPurger.size());
        return playerIdsAPurger.size();
    }
}