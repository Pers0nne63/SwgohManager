package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.TbMSStatsProjection;
import swgohManager.controller.dto.TbMissionHistoriqueProjection;
import swgohManager.controller.dto.TbMissionJoueurStatsProjection;
import swgohManager.controller.dto.TbParticipantProjection;
import swgohManager.controller.dto.TbRoundPlayerStatsProjection;
import swgohManager.controller.dto.TbRoundStatsProjection;
import swgohManager.model.TbActivite;
import swgohManager.model.TbScoreJoueur;

public interface TbScoreJoueurRepository extends JpaRepository<TbScoreJoueur, Long> {
    
    List<TbScoreJoueur> findByTbActivite(TbActivite tbActivite);
    
    @Query(value = """
            SELECT
                tb.id AS territoryBattleId,
                tb.end_time AS endTime,
                tb.total_stars AS totalStars,
                ta.round_num AS roundNum,
                SUM(CASE WHEN ta.stat_type = 'strike_encounter' THEN tsj.score ELSE 0 END) AS vagues,
                SUM(CASE WHEN ta.stat_type = 'covert_attempt' THEN tsj.score ELSE 0 END) AS msTentees
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            JOIN territory_battle tb ON tb.id = ta.territory_battle_id
            WHERE ta.round_num IS NOT NULL
              AND (:playerId IS NULL OR tsj.player_id = :playerId)
              AND tb.id IN (SELECT id FROM territory_battle ORDER BY end_time DESC LIMIT 5)
            GROUP BY tb.id, tb.end_time, tb.total_stars, ta.round_num
            ORDER BY tb.end_time, ta.round_num
            """, nativeQuery = true)
    List<TbRoundStatsProjection> findTbRoundStats(@Param("playerId") String playerId);

    @Query(value = """
            SELECT 
                bt.end_time AS endTime,
                j.player_name AS playerName, 
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase01_conflict03_covert01' THEN 1 ELSE 0 END) AS qiraT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase01_conflict03_covert01' THEN 1 ELSE 0 END) AS qiraW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase02_conflict01_covert01' THEN 1 ELSE 0 END) AS jkckT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase02_conflict01_covert01' THEN 1 ELSE 0 END) AS jkckW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict01_covert01' THEN 1 ELSE 0 END) AS sawT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict01_covert01' THEN 1 ELSE 0 END) AS sawW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert01' THEN 1 ELSE 0 END) AS revaT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict03_covert01' THEN 1 ELSE 0 END) AS revaW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert02' THEN 1 ELSE 0 END) AS bkmT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict03_covert02' THEN 1 ELSE 0 END) AS bkmW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict02_covert01' THEN 1 ELSE 0 END) AS merrinT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict02_covert01' THEN 1 ELSE 0 END) AS merrinW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict01_bonus_covert01' THEN 1 ELSE 0 END) AS clonesT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict01_bonus_covert01' THEN 1 ELSE 0 END) AS clonesW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase04_conflict02_covert01' THEN 1 ELSE 0 END) AS inquisT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase04_conflict02_covert01' THEN 1 ELSE 0 END) AS inquisW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase04_conflict03_covert01' THEN 1 ELSE 0 END) AS l337T,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase04_conflict03_covert01' THEN 1 ELSE 0 END) AS l337W,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase05_conflict03_covert01' THEN 1 ELSE 0 END) AS yhanT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase05_conflict03_covert01' THEN 1 ELSE 0 END) AS yhanW
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            JOIN joueurs j ON tsj.player_id = j.player_id
            JOIN territory_battle bt ON bt.id = ta.territory_battle_id
            WHERE (:playerId IS NULL OR tsj.player_id = :playerId) AND bt.id IN (SELECT id from territory_battle ORDER BY end_time DESC LIMIT 5)
            GROUP BY bt.end_time, j.player_name
            ORDER BY j.player_name, bt.end_time DESC LIMIT 5
            """, nativeQuery = true)
    List<TbMSStatsProjection> findPlayerTbMSStats(@Param("playerId") String playerId);
    
    @Query(value = """
            SELECT 
                bt.end_time AS endTime, 
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase01_conflict03_covert01' THEN 1 ELSE 0 END) AS qiraT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase01_conflict03_covert01' THEN 1 ELSE 0 END) AS qiraW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase02_conflict01_covert01' THEN 1 ELSE 0 END) AS jkckT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase02_conflict01_covert01' THEN 1 ELSE 0 END) AS jkckW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict01_covert01' THEN 1 ELSE 0 END) AS sawT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict01_covert01' THEN 1 ELSE 0 END) AS sawW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert01' THEN 1 ELSE 0 END) AS revaT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict03_covert01' THEN 1 ELSE 0 END) AS revaW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert02' THEN 1 ELSE 0 END) AS bkmT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict03_covert02' THEN 1 ELSE 0 END) AS bkmW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict02_covert01' THEN 1 ELSE 0 END) AS merrinT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict02_covert01' THEN 1 ELSE 0 END) AS merrinW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase03_conflict01_bonus_covert01' THEN 1 ELSE 0 END) AS clonesT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase03_conflict01_bonus_covert01' THEN 1 ELSE 0 END) AS clonesW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase04_conflict02_covert01' THEN 1 ELSE 0 END) AS inquisT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase04_conflict02_covert01' THEN 1 ELSE 0 END) AS inquisW,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase04_conflict03_covert01' THEN 1 ELSE 0 END) AS l337T,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase04_conflict03_covert01' THEN 1 ELSE 0 END) AS l337W,
                SUM(CASE WHEN ta.map_stat_id ='covert_round_attempted_mission_tb3_mixed_phase05_conflict03_covert01' THEN 1 ELSE 0 END) AS yhanT,
                SUM(CASE WHEN ta.map_stat_id ='covert_complete_mission_tb3_mixed_phase05_conflict03_covert01' THEN 1 ELSE 0 END) AS yhanW
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            JOIN joueurs j ON tsj.player_id = j.player_id
            JOIN territory_battle bt ON bt.id = ta.territory_battle_id
            WHERE bt.id IN (SELECT id from territory_battle ORDER BY end_time DESC LIMIT 5)
            GROUP BY bt.end_time
            ORDER BY bt.end_time DESC LIMIT 5
            """, nativeQuery = true)
    List<TbMSStatsProjection> findGuildTbMSStats();
    
    @Query(value = """
    	    SELECT tsj.player_id AS playerId,
    	        SUM(CASE WHEN ta.stat_type = 'power' THEN tsj.score ELSE 0 END) AS power,
    	        SUM(CASE WHEN ta.stat_type = 'summary' THEN tsj.score ELSE 0 END) AS summary,
    	        SUM(CASE WHEN ta.stat_type = 'strike_attempt' THEN tsj.score ELSE 0 END) AS strikeAttempt,
    	        SUM(CASE WHEN ta.stat_type = 'strike_encounter' THEN tsj.score ELSE 0 END) AS strikeEncounter,
    	        SUM(CASE WHEN ta.stat_type = 'covert_attempt' THEN tsj.score ELSE 0 END) AS covertAttempt
    	    FROM tb_score_joueur tsj
    	    JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
    	    WHERE ta.territory_battle_id = :tbId AND ta.round_num = :roundNum
    	    GROUP BY tsj.player_id
    	    """, nativeQuery = true)
    	List<TbRoundPlayerStatsProjection> findStatsParRoundEtTb(@Param("tbId") Long tbId, @Param("roundNum") Integer roundNum);

    @Query(value = """
            SELECT 
                ta.round_num AS roundNum,
                SUM(CASE WHEN ta.stat_type = 'power' THEN tsj.score ELSE 0 END) AS power,
                SUM(CASE WHEN ta.stat_type = 'summary' THEN tsj.score ELSE 0 END) AS summary,
                SUM(CASE WHEN ta.stat_type = 'strike_attempt' THEN tsj.score ELSE 0 END) AS strikeAttempt,
                SUM(CASE WHEN ta.stat_type = 'strike_encounter' THEN tsj.score ELSE 0 END) AS strikeEncounter,
                SUM(CASE WHEN ta.stat_type = 'covert_attempt' THEN tsj.score ELSE 0 END) AS covertAttempt
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            WHERE ta.territory_battle_id = :tbId AND ta.round_num IS NOT NULL
            GROUP BY ta.round_num
            ORDER BY ta.round_num ASC
            """, nativeQuery = true)
    List<TbRoundPlayerStatsProjection> findSyntheseGlobaleParRound(@Param("tbId") Long tbId);

    @Query(value = """
            SELECT tsj.player_id AS playerId,
                SUM(CASE WHEN ta.map_stat_id = :attemptedId THEN tsj.score ELSE 0 END) AS tentes,
                SUM(CASE WHEN ta.map_stat_id = :completedId THEN tsj.score ELSE 0 END) AS reussis
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            WHERE ta.territory_battle_id = :tbId
              AND ta.map_stat_id IN (:attemptedId, :completedId)
            GROUP BY tsj.player_id
            """, nativeQuery = true)
    List<TbMissionJoueurStatsProjection> findStatsMissionParTb(@Param("tbId") Long tbId,
                                                                @Param("attemptedId") String attemptedId,
                                                                @Param("completedId") String completedId);

    @Query(value = """
            SELECT id FROM territory_battle
            WHERE end_time < (SELECT end_time FROM territory_battle WHERE id = :tbId)
            ORDER BY end_time DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Long> findTbIdsPrecedentes(@Param("tbId") Long tbId);

    @Query(value = """
            SELECT ta.territory_battle_id AS territoryBattleId,
                tsj.player_id AS playerId,
                SUM(CASE WHEN ta.map_stat_id = :attemptedId THEN tsj.score ELSE 0 END) AS tentes,
                SUM(CASE WHEN ta.map_stat_id = :completedId THEN tsj.score ELSE 0 END) AS reussis
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            WHERE ta.territory_battle_id IN (:tbIds)
              AND ta.map_stat_id IN (:attemptedId, :completedId)
            GROUP BY ta.territory_battle_id, tsj.player_id
            """, nativeQuery = true)
    List<TbMissionHistoriqueProjection> findStatsMissionHistorique(@Param("tbIds") List<Long> tbIds,
                                                                     @Param("attemptedId") String attemptedId,
                                                                     @Param("completedId") String completedId);
    
    @Query(value = """
            SELECT DISTINCT tsj.player_id AS playerId, j.player_name AS playerName
            FROM tb_score_joueur tsj
            JOIN tb_activite ta ON ta.id = tsj.tb_activite_id
            JOIN joueurs j ON j.player_id = tsj.player_id
            WHERE ta.territory_battle_id = :tbId
            """, nativeQuery = true)
    List<TbParticipantProjection> findParticipantsTb(@Param("tbId") Long tbId);
    
}