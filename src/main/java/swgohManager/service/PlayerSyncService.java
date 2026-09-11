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
import swgohManager.model.SyncExecution;
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
import swgohManager.repository.PlanFarmIndRepository;
import swgohManager.repository.SyncExecutionRepository;

import swgohManager.service.JoueurService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerSyncService {

    private final SwgohApiClient swgohApiClient;
    private final SyncExecutionRepository syncExecutionRepository;

    // Services communs (déjà unifiés avec Portee)
    private final PlayerRatingService playerRatingService;
    private final RosterUnitService rosterUnitService;
    private final PlayerDatacronService playerDatacronService;
    private final PlayerEraUnitStatusService playerEraUnitStatusService;
    private final RosterUnitStatCalculService rosterUnitStatCalculService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;
    private final StatqCalculService statqCalculService;
    private final UnitModCalculationService unitModCalculationService;

    // ---- Spécifique guilde ----
    private final PlanFarmIndRepository planFarmIndRepository;
    private final FarmPlanIndProgressService farmPlanIndProgressService;
    private final JoueurService joueurService;

    // ---- Spécifique externe ----
    private final ExternalPlayerRepository externalPlayerRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalRosterUnitModActuelRepository externalRosterUnitModActuelRepository;
    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final PlayerModQService playerModQService;
    // Purge externe
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
    private final ExternalPlayerRatingActuelRepository externalPlayerRatingActuelRepository;

    // =========================================================================
    // GUILDE
    // =========================================================================

    /** Appel isolé (un seul joueur) : crée son propre idSync. */
    public PlayerSyncResult synchroniserJoueur(PlayerIdentifier identifier) {
        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();
        return synchroniserJoueur(identifier, idSync);
    }

    /** Appel groupé : reçoit un idSync déjà créé par l'orchestrateur, partagé entre tous les joueurs du lot. */
    public PlayerSyncResult synchroniserJoueur(PlayerIdentifier identifier, Long idSync) {
        log.info("Appel API /player pour {}", identifier);
        PlayerResponse response = swgohApiClient.getPlayer(identifier);

        joueurService.mettreAJourDepuisPlayer(response);
        
        RatingCalculationService.RatingResult rating = playerRatingService.enregistrerRating(response, Portee.GUILDE);
        String resultatRoster = rosterUnitService.enregistrerRoster(response, idSync);

        playerDatacronService.enregistrer(response.playerId(), response, Portee.GUILDE);
        playerEraUnitStatusService.enregistrer(response.playerId(), response, Portee.GUILDE);

        if (!planFarmIndRepository.findByPlayerId(response.playerId()).isEmpty()) {
            farmPlanIndProgressService.calculerEtEnregistrer(response.playerId(), idSync);
        }

        String message = String.format("Joueur %s (%s) : skillRating=%s, ligue=%s, division=%s | %s",
                response.name(), response.playerId(),
                rating != null ? rating.skillRating() : "n/a",
                rating != null ? rating.leagueId() : "n/a",
                rating != null ? rating.divisionId() : "n/a",
                resultatRoster);

        log.info(message);
        return new PlayerSyncResult(response.playerId(), response.name(), message);
    }

    public record PlayerSyncResult(String playerId, String playerName, String message) {}

    // =========================================================================
    // EXTERNE
    // =========================================================================

    /**
     * Scanne un joueur externe (hors guilde) par ally code.
     * Depuis que characterGalacticPower/galacticPower/shipGalacticPower sont extraits
     * directement de PlayerResponse (sans dépendre d'un appel /guild), le flux suit le même
     * ordre logique que la synchro guilde : le scan de guilde annexe ne sert plus qu'à
     * récupérer les scores raid/TB du joueur, pas à débloquer le calcul de ModQ/stats.
     */
    @Transactional
    public String scannerExterneParPlayerId(String playerId) {
        return scannerExterne(PlayerIdentifier.of(playerId, null));
    }
    
    @Transactional
    public String scannerExterne(PlayerIdentifier identifier) {
        PlayerResponse response = swgohApiClient.getPlayer(identifier);
        String playerId = response.playerId();
        
        //On nettoie d'abord les tables existantes si le joueur avait déjà été scanné
        
        purgerListe(List.of(playerId));
        externalPlayerRepository.flush();
        
        //on scanne le joueur

        ExternalPlayer externalPlayer = ExternalPlayer.builder()
                .playerId(playerId)
                .allyCode(response.allyCode())
                .playerName(response.name())
                .guildId(response.guildId())
                .guildName(response.guildName())
                .galacticPower(parseLongOrNull(response.getGalacticPower()))
                .characterGalacticPower(parseLongOrNull(response.getCharacterGalacticPower()))
                .shipGalacticPower(parseLongOrNull(response.getShipGalacticPower()))
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

        externalRosterUnitActuelRepository.saveAll(unites);
        externalRosterUnitModActuelRepository.saveAll(mods);

        Map<String, SkillDefinition> definitions = rosterUnitService.chargerDefinitionsSkill();
        rosterUnitService.enregistrerSkills(playerId, response.rosterUnit(), definitions, Portee.EXTERNE, null);
        playerDatacronService.enregistrer(playerId, response, Portee.EXTERNE);

        playerModQService.calculerEtEnregistrerExterne(playerId, mods, externalPlayer.getCharacterGalacticPower());

        rosterUnitStatCalculService.calculerEtEnregistrerExterne(playerId, unites, mods);
        rosterUnitStatObjectifService.calculerEtEnregistrerExterne(playerId, unites);
        statqCalculService.calculerEtEnregistrer(playerId);

        playerEraUnitStatusService.enregistrer(playerId, response, Portee.EXTERNE);

        String message = String.format("Scan externe : %s (%s) — %d unité(s), %d ligne(s) de mod",
                response.name(), playerId, unites.size(), mods.size());
        log.info(message);
        return playerId;
    }

    private Long parseLongOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Purge des scans externes de plus de 30 jours (joueur + roster + mods + modQ + raid + TB + skills + datacrons + stats). */
    
    @Transactional
    public int purgerParGuildId(String guildId) {
        List<String> playerIds = externalPlayerRepository.findPlayerIdsByGuildId(guildId);
        purgerListe(playerIds);
        log.info("Purge des scans externes pour guildId={} : {} joueur(s) supprimé(s)", guildId, playerIds.size());
        return playerIds.size();
    }

    @Transactional
    public int purgerAnciensScan() {
        Instant seuil = Instant.now().minus(30, ChronoUnit.DAYS);
        List<String> playerIdsAPurger = externalPlayerRepository.findPlayerIdsScannesAvant(seuil);
        purgerListe(playerIdsAPurger);
        log.info("Purge des scans externes : {} joueur(s) supprimé(s)", playerIdsAPurger.size());
        return playerIdsAPurger.size();
    }

    private void purgerListe(List<String> playerIds) {
        for (String playerId : playerIds) {
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
    }
}