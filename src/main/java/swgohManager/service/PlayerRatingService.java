package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.ExternalPlayerRatingActuel;
import swgohManager.model.PlayerRatingHistorique;
import swgohManager.repository.ExternalPlayerRatingActuelRepository;
import swgohManager.repository.PlayerRatingHistoriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerRatingService {

    private final PlayerRatingHistoriqueRepository playerRatingHistoriqueRepository;
    private final ExternalPlayerRatingActuelRepository externalPlayerRatingActuelRepository;
    private final RatingCalculationService ratingCalculationService;

    /**
     * Enregistre le rating d'un joueur.
     * Portee.GUILDE : ajoute une ligne à l'historique (comportement inchangé, une ligne par sync).
     * Portee.EXTERNE : upsert dans la table "actuel" externe (pas d'historique).
     * @return l'entité sauvegardée (PlayerRatingHistorique ou ExternalPlayerRatingActuel selon la portée), ou null si pas de donnée.
     */
    @Transactional
    public RatingCalculationService.RatingResult enregistrerRating(PlayerResponse response, Portee portee) {
        RatingCalculationService.RatingResult r = ratingCalculationService.calculer(response);

        if (r == null) {
            log.warn("Aucune donnée playerRating pour le joueur {}", response.playerId());
            return null;
        }

        if (portee == Portee.GUILDE) {
            playerRatingHistoriqueRepository.save(PlayerRatingHistorique.builder()
                    .playerId(response.playerId())
                    .skillRating(r.skillRating())
                    .leagueId(r.leagueId())
                    .divisionId(r.divisionId())
                    .build());
            log.info("Rating enregistré pour {} : {} points, ligue {} division {}",
                    response.playerId(), r.skillRating(), r.leagueId(), r.divisionId());
        } else {
            ExternalPlayerRatingActuel existant = externalPlayerRatingActuelRepository
                    .findByPlayerId(response.playerId()).orElse(new ExternalPlayerRatingActuel());
            existant.setPlayerId(response.playerId());
            existant.setSkillRating(r.skillRating());
            existant.setLeagueId(r.leagueId());
            existant.setDivisionId(r.divisionId());
            externalPlayerRatingActuelRepository.save(existant);
            log.info("Rating externe enregistré pour {} : {} points, ligue {} division {}",
                    response.playerId(), r.skillRating(), r.leagueId(), r.divisionId());
        }

        return r;
    }
}