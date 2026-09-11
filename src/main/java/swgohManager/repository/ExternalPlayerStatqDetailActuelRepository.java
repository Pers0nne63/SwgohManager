package swgohManager.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.RosterIdUnitProjection;
import swgohManager.controller.dto.StatQTeamProjection;
import swgohManager.model.ExternalPlayerStatqDetailActuel;
import swgohManager.repository.PlayerStatqDetailActuelRepository.StatqTeamAverageProjection;
public interface ExternalPlayerStatqDetailActuelRepository extends JpaRepository<ExternalPlayerStatqDetailActuel, Long> {
    List<ExternalPlayerStatqDetailActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = """
            SELECT ru.player_id AS playerId, ud.base_id AS baseId, ru.id_unit AS idUnit
            FROM external_roster_unit_actuel ru
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            WHERE ru.player_id = :playerId
            """, nativeQuery = true)
    List<RosterIdUnitProjection> findIdUnitParBaseId(@Param("playerId") String playerId);
    
    @Query(value = """
            SELECT sd.player_id AS playerId, sd.team AS team, SUM(sd.note) AS note, AVG(sd.note) as avgNote
            FROM external_player_statq_detail_actuel sd
            WHERE sd.player_id = :playerId
            GROUP BY sd.player_id, sd.team
            ORDER BY AVG(sd.note) DESC
            """, nativeQuery = true)
    List<StatQTeamProjection> findStatQbyTeambyPlayerId(@Param("playerId") String playerId);
    
    @Query(value = """
    	    SELECT d.team AS team, AVG(d.note) AS moyenneNote
    	    FROM external_player_statq_detail_actuel d
    	    JOIN external_player p ON p.player_id = d.player_id
    	    WHERE p.guild_id = :guildId
    	    GROUP BY d.team ORDER BY d.team
    	    """, nativeQuery = true)
    	List<StatqTeamAverageProjection> findMoyenneNoteParTeamByGuildId(@Param("guildId") String guildId);
}