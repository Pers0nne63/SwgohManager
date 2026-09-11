package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.PlayerStatqDetailActuel;

public interface PlayerStatqDetailActuelRepository extends JpaRepository<PlayerStatqDetailActuel, Long> {
    List<PlayerStatqDetailActuel> findByPlayerId(String playerId);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    
    public interface StatqTeamAverageProjection {
        String getTeam();
        Double getMoyenneNote();
    }

    @Query(value = """
        SELECT d.team AS "team", AVG(d.note) AS "moyenneNote"
        FROM player_statq_detail_actuel d
        JOIN joueurs j ON j.player_id = d.player_id
        WHERE j.present_in_guild = true
        GROUP BY d.team ORDER BY d.team
        """, nativeQuery = true)
    List<StatqTeamAverageProjection> findMoyenneNoteParTeam();
}