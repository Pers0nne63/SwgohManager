package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.PlayerDatacronMecaniqueCheckProjection;
import swgohManager.controller.dto.PlayerDatacronProjection;
import swgohManager.controller.dto.PlayerDatacronStatSumProjection;
import swgohManager.model.PlayerDatacronAffixActuel;

public interface PlayerDatacronAffixActuelRepository extends JpaRepository<PlayerDatacronAffixActuel, Long> {

    void deleteByPlayerId(String playerId);
    List<PlayerDatacronAffixActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);

    @Query(value = """
        SELECT 
            pda.player_id AS "playerId", 
            pda.id_datacron AS "idDatacron", 
            pda.focused AS "focused",            
            case when pda.template_id like '%focused%' then SPLIT_PART(pda.template_id,'_',5) else null end as "focusLibelle",
            pda.set_id AS "setId", 
            CASE 
                WHEN pdaa.ordre IN (3, 6, 9, 12, 15) then pdaa.ordre else 0 end as "ordre",
            CASE WHEN SPLIT_PART(pdaa.target_rule, '_', 3) = 'lightside' then 'Côté Lumineux'
    			when SPLIT_PART(pdaa.target_rule, '_', 3) = 'darkside' then 'Côté Obscur'
    			else SPLIT_PART(pdaa.target_rule, '_', 3) end as Target,
            CASE 
                WHEN pdaa.ordre IN (3, 6, 9, 12, 15) THEN 
                    REPLACE(
                        ad.description, 
                        '{0}', 
                        CASE 
                            WHEN pdaa.ability_id LIKE 'datacron_faction%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)
                            WHEN pdaa.ability_id LIKE 'datacron_alignment%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)
                            WHEN pdaa.ability_id LIKE 'datacron_character%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)  
                            ELSE NULL 
                        END
                    )
                ELSE sd."libellé" 
            END AS "description",
            SUM(ROUND(cast(pdaa.stat_value as numeric) / 1000000,2)) AS "value"
        FROM player_datacron_affix_actuel pdaa 
        LEFT OUTER JOIN player_datacron_actuel pda ON pdaa.id_datacron = pda.id_datacron 
        LEFT OUTER JOIN joueurs j ON pda.player_id = j.player_id 
        LEFT OUTER JOIN stat_definition sd ON pdaa.stat_type = sd.stat_id 
        LEFT OUTER JOIN ability_definition ad ON ad.id = pdaa.ability_id 
        WHERE j.player_id = :playerId
        GROUP BY 
            pda.player_id, 
            pda.id_datacron,
            pda.focused, 
            pda.template_id,
            pda.set_id, 
            CASE 
                WHEN pdaa.ordre IN (3, 6, 9, 12, 15) then pdaa.ordre else 0 end,
            CASE WHEN SPLIT_PART(pdaa.target_rule, '_', 3) = 'lightside' then 'Côté Lumineux'
    			when SPLIT_PART(pdaa.target_rule, '_', 3) = 'darkside' then 'Côté Obscur'
    			else SPLIT_PART(pdaa.target_rule, '_', 3) end,
            CASE 
                WHEN pdaa.ordre IN (3, 6, 9, 12, 15) THEN 
                    REPLACE(
                        ad.description, 
                        '{0}', 
                        CASE 
                            WHEN pdaa.ability_id LIKE 'datacron_faction%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)
                            WHEN pdaa.ability_id LIKE 'datacron_alignment%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)
                            WHEN pdaa.ability_id LIKE 'datacron_character%' THEN SPLIT_PART(pdaa.target_rule, '_', 3)  
                            ELSE NULL 
                        END
                    )
                ELSE sd."libellé" 
            END
        ORDER BY pda.player_id, pda.id_datacron
        """, nativeQuery = true)
    List<PlayerDatacronProjection> findPlayersDatacron(@Param("playerId") String playerId);
    
    @Query(value = """
    	    SELECT pda.player_id AS "playerId", pda.id_datacron AS "idDatacron", pda.set_id AS "setId",
    	           pdaa.ordre AS "tier", pdaa.ability_id AS "abilityId"
    	    FROM player_datacron_affix_actuel pdaa
    	    LEFT JOIN player_datacron_actuel pda ON pda.id_datacron = pdaa.id_datacron
    	    WHERE pdaa.ordre IN (3,6,9,12,15) AND pdaa.ability_id IS NOT NULL
    	    GROUP BY pda.player_id, pda.id_datacron, pda.set_id, pdaa.ordre, pdaa.ability_id
    	    """, nativeQuery = true)
    	List<PlayerDatacronMecaniqueCheckProjection> findMecaniquesEquipeesParJoueur();

    	@Query(value = """
    	    SELECT pda.player_id AS "playerId", pda.id_datacron AS "idDatacron", pda.set_id AS "setId",
    	           pdaa.stat_type AS "statType", SUM(ROUND(cast(pdaa.stat_value as numeric)/1000000,2)) AS "value"
    	    FROM player_datacron_affix_actuel pdaa
    	    LEFT JOIN player_datacron_actuel pda ON pda.id_datacron = pdaa.id_datacron
    	    WHERE pdaa.stat_type IS NOT NULL
    	    GROUP BY pda.player_id, pda.id_datacron, pda.set_id, pdaa.stat_type
    	    """, nativeQuery = true)
    	List<PlayerDatacronStatSumProjection> findSommeStatsParJoueur();
}