package swgohManager.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.PlayerRatingHistorique;
import swgohManager.model.SyncExecution;
import swgohManager.repository.PlanFarmIndRepository;
import swgohManager.repository.SyncExecutionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerSyncService {

    private final SwgohApiClient swgohApiClient;
    private final PlayerRatingService playerRatingService;
    private final RosterUnitService rosterUnitService;
    private final SyncExecutionRepository syncExecutionRepository;

    // Dépendances injectées pour le plan individuel
    private final PlanFarmIndRepository planFarmIndRepository;
    private final FarmPlanIndProgressService farmPlanIndProgressService;
    
    // Nouvelles dépendances injectées
    private final PlayerDatacronService playerDatacronService;
    private final PlayerEraUnitStatusService playerEraUnitStatusService;

    /** Appel isolé (un seul joueur) : crée son propre idSync. */
    public PlayerSyncResult synchroniserJoueur(PlayerIdentifier identifier) {
        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();
        return synchroniserJoueur(identifier, idSync);
    }

    /** Appel groupé : reçoit un idSync déjà créé par l'orchestrateur, partagé entre tous les joueurs du lot. */
    public PlayerSyncResult synchroniserJoueur(PlayerIdentifier identifier, Long idSync) {
        log.info("Appel API /player pour {}", identifier);
        PlayerResponse response = swgohApiClient.getPlayer(identifier);

        PlayerRatingHistorique rating = playerRatingService.enregistrerRating(response);
        String resultatRoster = rosterUnitService.enregistrerRoster(response, idSync);

        // Enregistrement des datacrons et des statuts d'ère
        playerDatacronService.enregistrer(response.playerId(), response);
        playerEraUnitStatusService.enregistrer(response.playerId(), response);

        // Calcul et enregistrement de la progression si le joueur a au moins un objectif
        if (!planFarmIndRepository.findByPlayerId(response.playerId()).isEmpty()) {
            farmPlanIndProgressService.calculerEtEnregistrer(response.playerId(), idSync);
        }

        String message = String.format("Joueur %s (%s) : skillRating=%s, ligue=%s, division=%s | %s",
                response.name(), response.playerId(),
                rating != null ? rating.getSkillRating() : "n/a",
                rating != null ? rating.getLeagueId() : "n/a",
                rating != null ? rating.getDivisionId() : "n/a",
                resultatRoster);

        log.info(message);
        return new PlayerSyncResult(response.playerId(), response.name(), message);
    }

    public record PlayerSyncResult(String playerId, String playerName, String message) {}
}