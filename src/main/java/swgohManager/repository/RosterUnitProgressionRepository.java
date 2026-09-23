package swgohManager.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.RosterProgressionJourProjection;
import swgohManager.model.RosterUnitProgression;

public interface RosterUnitProgressionRepository extends JpaRepository<RosterUnitProgression, Long> {
    List<RosterUnitProgression> findByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT
    	        CAST(rup.date_constat AS DATE) AS "jour",
    	        rup.player_id AS "playerId",
    	        j.player_name AS "playerName",
    	        ud.base_id AS "baseId",
    	        ud.libelle AS "libelle",
    	        BOOL_OR(rup.nouvelle_unite) AS "nouvelleUnite",
    	        MIN(rup.etoiles_avant) AS "etoilesAvant", MAX(rup.etoiles_apres) AS "etoilesApres",
    	        MIN(rup.gear_avant) AS "gearAvant", MAX(rup.gear_apres) AS "gearApres",
    	        MIN(rup.relic_avant) AS "relicAvant", MAX(rup.relic_apres) AS "relicApres"
    	    FROM roster_unit_progression rup
    	    JOIN unit_definition ud ON ud.id_unit = rup.definition_id
    	    LEFT JOIN joueurs j ON j.player_id = rup.player_id
    	    WHERE rup.date_constat BETWEEN :debut AND :fin
    	      AND NOT EXISTS (
    	          SELECT 1 FROM player_era_unit_status_actuel peusa
    	          WHERE peusa.player_id = rup.player_id
    	            AND peusa.unit_base_id = ud.base_id
    	      )
    	    GROUP BY CAST(rup.date_constat AS DATE), rup.player_id, j.player_name, ud.base_id, ud.libelle
    	    ORDER BY jour DESC, "playerName"
    	    """, nativeQuery = true)
    	List<RosterProgressionJourProjection> findProgressionParJour(@Param("debut") Instant debut, @Param("fin") Instant fin);
    
    @Query(value = """
    	    SELECT
    	        CAST(rup.date_constat AS DATE) AS "jour",
    	        rup.player_id AS "playerId",
    	        j.player_name AS "playerName",
    	        ud.base_id AS "baseId",
    	        ud.libelle AS "libelle",
    	        BOOL_OR(rup.nouvelle_unite) AS "nouvelleUnite",
    	        MIN(rup.etoiles_avant) AS "etoilesAvant", MAX(rup.etoiles_apres) AS "etoilesApres",
    	        MIN(rup.gear_avant) AS "gearAvant", MAX(rup.gear_apres) AS "gearApres",
    	        MIN(rup.relic_avant) AS "relicAvant", MAX(rup.relic_apres) AS "relicApres"
    	    FROM roster_unit_progression rup
    	    JOIN unit_definition ud ON ud.id_unit = rup.definition_id
    	    LEFT JOIN joueurs j ON j.player_id = rup.player_id
    	    WHERE rup.player_id = :playerId
    	      AND rup.date_constat BETWEEN :debut AND :fin
    	      AND NOT EXISTS (
    	          SELECT 1 FROM player_era_unit_status_actuel peusa
    	          WHERE peusa.player_id = rup.player_id
    	            AND peusa.unit_base_id = ud.base_id
    	      )
    	    GROUP BY CAST(rup.date_constat AS DATE), rup.player_id, j.player_name, ud.base_id, ud.libelle
    	    ORDER BY jour DESC, ud.libelle
    	    """, nativeQuery = true)
    	List<RosterProgressionJourProjection> findProgressionParJourPourJoueur(
    	        @Param("playerId") String playerId, @Param("debut") Instant debut, @Param("fin") Instant fin);
}