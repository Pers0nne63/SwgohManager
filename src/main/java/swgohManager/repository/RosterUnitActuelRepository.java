package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.GuildeRelicRepartitionProjection;
import swgohManager.controller.dto.RelicJoueurProjection;
import swgohManager.controller.dto.RelicR8CountProjection;
import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.controller.dto.RosterIdUnitProjection;
import swgohManager.controller.dto.RosterUnitCardProjection;
import swgohManager.model.RosterUnitActuel;

public interface RosterUnitActuelRepository extends JpaRepository<RosterUnitActuel, Long> {
    List<RosterUnitActuel> findByPlayerId(String playerId);
    
    @Modifying
    @Query("DELETE FROM RosterUnitActuel r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);

    @Modifying
    @Query("DELETE FROM RosterUnitActuel r WHERE r.playerId NOT IN :activePlayerIds")
    void deleteByPlayerIdNotIn(@Param("activePlayerIds") List<String> activePlayerIds);
    
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
    
    @Query(value = """
            SELECT 
                ru.player_id AS "playerId", 
                j.player_name AS "playerName", 
                ud.base_id AS "baseId", 
                ud.libelle AS "libelle", 
                ru.relic AS "relic"
            FROM roster_unit_actuel ru
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            LEFT JOIN joueurs j ON ru.player_id = j.player_id
            WHERE ud.base_id IN :baseIds
            """, nativeQuery = true)
    List<RelicJoueurProjection> findRelicsPourUnites(@Param("baseIds") List<String> baseIds);
    
    @Query(value = """
            SELECT AVG(ru.relic)
            FROM roster_unit_actuel ru
            WHERE ru.player_id = :playerId AND ru.relic >= 0
            """, nativeQuery = true)
    Double findRelicMoyenJoueur(@Param("playerId") String playerId);

    @Query(value = """
            SELECT DISTINCT ud.base_id
            FROM roster_unit_actuel ru
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            WHERE ru.player_id = :playerId AND ud.legend = true
            """, nativeQuery = true)
    List<String> findBaseIdsLegendPossedes(@Param("playerId") String playerId);

    @Query(value = """
            SELECT DISTINCT ud.base_id
            FROM roster_unit_actuel ru
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            WHERE ru.player_id = :playerId AND ud.conquete = true
            """, nativeQuery = true)
    List<String> findBaseIdsConquetePossedes(@Param("playerId") String playerId);
    
    @Query(value = """
            SELECT ru.player_id AS "playerId", COUNT(*) AS "nbR8Plus"
            FROM roster_unit_actuel ru
            WHERE ru.relic >= 8
            GROUP BY ru.player_id
            """, nativeQuery = true)
    List<RelicR8CountProjection> countUnitesR8PlusParJoueur();
    
    @Query(value = """
            SELECT 
                ud.base_id AS baseId,
                ru.id_unit AS idUnit,
                ud.libelle AS libelle,
                REPLACE(ud.thumbnail_name, 'tex.', '') AS portrait,
                ud.role AS role,
                ud.role AS role,
    			ud.leader AS leader,
    			ud.factions AS factions,
                ud.force_alignment AS forceAlignment,
                ud.legend AS legend,
                ud.conquete AS conquete,
                ud.combat_type AS combatType,
                ru.etoiles AS etoiles,
                ru.gear AS gear,
                ru.relic AS relic,
                ru.ultimate AS ultimate,
                COALESCE(z.nb_zetas, 0) AS nbZetas,
                COALESCE(o.nb_omicrons, 0) AS nbOmicrons
            FROM roster_unit_actuel ru
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            LEFT JOIN (
                SELECT rus.id_unit, COUNT(*) AS nb_zetas
                FROM roster_unit_skill_actuel rus
                JOIN skill_definition sd ON sd.id_skill = rus.id_skill
                WHERE rus.player_id = :playerId AND (rus.zeta_applied = true OR sd.skill_zeta = true)
                GROUP BY rus.id_unit
            ) z ON z.id_unit = ru.id_unit
            LEFT JOIN (
                SELECT rus.id_unit, COUNT(*) AS nb_omicrons
                FROM roster_unit_skill_actuel rus
                WHERE rus.player_id = :playerId AND rus.omicron_applied = true
                GROUP BY rus.id_unit
            ) o ON o.id_unit = ru.id_unit
            WHERE ru.player_id = :playerId AND ud.combat_type = 1
            ORDER BY 
                CASE WHEN ud.legend = true THEN 0 ELSE 1 END ASC,
                COALESCE(ru.relic, -1) DESC,
                COALESCE(ru.gear, 0) DESC,
                ud.libelle ASC
            """, nativeQuery = true)
    List<RosterUnitCardProjection> findRosterCartesJoueur(@Param("playerId") String playerId);

}