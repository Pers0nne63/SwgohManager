package swgohManager.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.OmicronDetailProjection;
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
    	        BOOL_OR(rup.omicron_obtenu) AS "omicronObtenu",
    	        MIN(rup.etoiles_avant) AS "etoilesAvant", MAX(rup.etoiles_apres) AS "etoilesApres",
    	        MIN(rup.gear_avant) AS "gearAvant", MAX(rup.gear_apres) AS "gearApres",
    	        MIN(rup.relic_avant) AS "relicAvant", MAX(rup.relic_apres) AS "relicApres"
    	    FROM roster_unit_progression rup
    	    JOIN unit_definition ud ON ud.id_unit = rup.definition_id
    	    LEFT JOIN joueurs j ON j.player_id = rup.player_id
    	    WHERE rup.date_constat BETWEEN :debut AND :fin
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
                BOOL_OR(rup.omicron_obtenu) AS "omicronObtenu",
                MIN(rup.etoiles_avant) AS "etoilesAvant", MAX(rup.etoiles_apres) AS "etoilesApres",
                MIN(rup.gear_avant) AS "gearAvant", MAX(rup.gear_apres) AS "gearApres",
                MIN(rup.relic_avant) AS "relicAvant", MAX(rup.relic_apres) AS "relicApres"
            FROM roster_unit_progression rup
            JOIN unit_definition ud ON ud.id_unit = rup.definition_id
            LEFT JOIN joueurs j ON j.player_id = rup.player_id
            WHERE rup.player_id = :playerId
              AND rup.date_constat BETWEEN :debut AND :fin
            GROUP BY CAST(rup.date_constat AS DATE), rup.player_id, j.player_name, ud.base_id, ud.libelle
            ORDER BY jour DESC, ud.libelle
            """, nativeQuery = true)
    List<RosterProgressionJourProjection> findProgressionParJourPourJoueur(
            @Param("playerId") String playerId, @Param("debut") Instant debut, @Param("fin") Instant fin);
    
    @Query(value = """
    	    SELECT rup.id_skill AS idSkill, rup.skill_type AS skillType, rup.skill_numero AS skillNumero
    	    FROM roster_unit_progression rup
    	    JOIN unit_definition ud ON ud.id_unit = rup.definition_id
    	    WHERE rup.player_id = :playerId
    	      AND ud.base_id = :baseId
    	      AND CAST(rup.date_constat AS DATE) = :jour
    	      AND rup.omicron_obtenu = true
    	    ORDER BY rup.skill_type, rup.skill_numero
    	    """, nativeQuery = true)
    	List<OmicronDetailProjection> findOmicronsObtenusJour(
    	        @Param("playerId") String playerId, @Param("baseId") String baseId, @Param("jour") LocalDate jour);
}