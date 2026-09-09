package swgohManager.service;

import org.springframework.stereotype.Service;
import swgohManager.client.dto.PlayerResponse;

/**
 * Calcul commun du parsing du rating (skillRating/ligue/division) à partir de la réponse Comlink,
 * utilisé par la persistance interne (avec historique) et externe (sans historique).
 */
@Service
public class RatingCalculationService {

    public record RatingResult(Integer skillRating, String leagueId, Integer divisionId) {}

    public RatingResult calculer(PlayerResponse response) {
        if (response.playerRating() == null) {
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

        return new RatingResult(skillRating, leagueId, division);
    }
}