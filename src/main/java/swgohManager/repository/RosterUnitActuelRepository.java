package swgohManager.repository;

import swgohManager.model.RosterUnitActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.controller.dto.RosterIdUnitProjection;
import swgohManager.controller.dto.GuildeRelicRepartitionProjection;

public interface RosterUnitActuelRepository extends JpaRepository<RosterUnitActuel, Long> {
    List<RosterUnitActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT ud.base_id AS baseId, MAX(ru.etoiles) AS maxEtoiles, MAX(ru.relic) AS maxRelic
    	    FROM roster_unit_actuel ru
    	    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
    	    WHERE ru.player_id = :playerId
    	    GROUP BY ud.base_id
    	    """, nativeQuery = true)
    	List<RosterBaseIdProgressProjection> findMaxEtoilesRelicByBaseId(@Param("playerId") String playerId);

    @Query(value = """
    	    SELECT ru.player_id AS playerId, ud.base_id AS baseId, ru.id_unit AS idUnit
    	    FROM roster_unit_actuel ru
    	    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
    	    """, nativeQuery = true)
    	List<RosterIdUnitProjection> findTousLesIdUnitParBaseId();
   

    @Query(value = """
            SELECT 
                SUM(CASE WHEN ru.relic = 10 THEN 1 ELSE 0 END) AS relic10,
                SUM(CASE WHEN ru.relic = 9 THEN 1 ELSE 0 END) AS relic9,
                SUM(CASE WHEN ru.relic = 8 THEN 1 ELSE 0 END) AS relic8,
                SUM(CASE WHEN ru.relic IN (6, 7) THEN 1 ELSE 0 END) AS relic6Et7,
                SUM(CASE WHEN ru.relic BETWEEN 0 AND 5 THEN 1 ELSE 0 END) AS relic0A5,
                SUM(CASE WHEN ru.relic = -1 THEN 1 ELSE 0 END) AS sansRelic
            FROM roster_unit_actuel ru
            WHERE ru.relic IS NOT NULL
            """, nativeQuery = true)
    GuildeRelicRepartitionProjection findRepartitionRelicsGuilde();
    
    @Query(value = """
            SELECT 
                SUM(CASE WHEN ru.relic = 10 THEN 1 ELSE 0 END) AS relic10,
                SUM(CASE WHEN ru.relic = 9 THEN 1 ELSE 0 END) AS relic9,
                SUM(CASE WHEN ru.relic = 8 THEN 1 ELSE 0 END) AS relic8,
                SUM(CASE WHEN ru.relic IN (6, 7) THEN 1 ELSE 0 END) AS relic6Et7,
                SUM(CASE WHEN ru.relic BETWEEN 0 AND 5 THEN 1 ELSE 0 END) AS relic0A5,
                SUM(CASE WHEN ru.relic = -1 THEN 1 ELSE 0 END) AS sansRelic
            FROM roster_unit_actuel ru
            WHERE ru.player_id = :playerId AND ru.relic IS NOT NULL
            """, nativeQuery = true)
    GuildeRelicRepartitionProjection findRepartitionRelicsJoueur(@Param("playerId") String playerId);

}