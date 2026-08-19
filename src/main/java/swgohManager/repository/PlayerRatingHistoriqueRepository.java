package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swgohManager.model.PlayerRatingHistorique;

public interface PlayerRatingHistoriqueRepository extends JpaRepository<PlayerRatingHistorique, Long> {

    List<PlayerRatingHistorique> findByPlayerIdOrderByDateReleveAsc(String playerId);

    // Interface pour la projection JPA
    interface DernierRatingProjection {
        String getPlayerId();
        Integer getRating();
    }

    // Récupère la dernière note enregistrée pour chaque joueur fourni
    @Query(value = """
        SELECT pr.player_id AS playerId, pr.skill_rating AS rating
        FROM player_rating_historique pr
        INNER JOIN (
            SELECT player_id, MAX(date_releve) AS max_date
            FROM player_rating_historique
            WHERE player_id IN (:playerIds)
            GROUP BY player_id
        ) max_pr ON pr.player_id = max_pr.player_id AND pr.date_releve = max_pr.max_date
        """, nativeQuery = true)
    List<DernierRatingProjection> findDernierRatingPourJoueurs(@Param("playerIds") List<String> playerIds);
}