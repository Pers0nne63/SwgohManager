package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.PlayerRatingHistorique;
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

    @Transactional
    public PlayerRatingHistorique enregistrerRating(PlayerResponse response) {
        if (response.playerRating() == null) {
            log.warn("Aucune donnée playerRating pour le joueur {}", response.playerId());
            return null;
        }

        Integer skillRating = response.playerRating().playerSkillRating() != null
                ? response.playerRating().playerSkillRating().skillRating() : null;

        String leagueId = response.playerRating().playerRankStatus() != null
                ? response.playerRating().playerRankStatus().leagueId() : null;

        Integer divisionBrute = response.playerRating().playerRankStatus() != null
                ? response.playerRating().playerRankStatus().divisionId() : null;

        // L'API renvoie la division par tranche de 5 (5 à 25), dans l'ordre inverse du jeu.
        // 25 -> 1, 20 -> 2, 15 -> 3, 10 -> 4, 5 -> 5.
        Integer division = divisionBrute != null ? (30 - divisionBrute) / 5 : null;

        PlayerRatingHistorique entree = PlayerRatingHistorique.builder()
                .playerId(response.playerId())
                .skillRating(skillRating)
                .leagueId(leagueId)
                .divisionId(division)
                .build();

        PlayerRatingHistorique sauvee = playerRatingHistoriqueRepository.save(entree);
        log.info("Rating enregistré pour {} : {} points, ligue {} division {}",
                response.playerId(), skillRating, leagueId, division);

        return sauvee;
    }
}