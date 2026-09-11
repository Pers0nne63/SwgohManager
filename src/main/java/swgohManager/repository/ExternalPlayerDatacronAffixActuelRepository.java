package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.PlayerDatacronMecaniqueCheckProjection;
import swgohManager.controller.dto.PlayerDatacronStatSumProjection;
import swgohManager.model.ExternalPlayerDatacronAffixActuel;

public interface ExternalPlayerDatacronAffixActuelRepository extends JpaRepository<ExternalPlayerDatacronAffixActuel, Long> {

    void deleteByPlayerId(String playerId);
    List<ExternalPlayerDatacronAffixActuel> findByPlayerId(String playerId);

    @Query(value = """
            SELECT pda.player_id AS "playerId", pda.id_datacron AS "idDatacron", pda.set_id AS "setId",
                   pdaa.ordre AS "tier", pdaa.ability_id AS "abilityId"
            FROM external_player_datacron_affix_actuel pdaa
            LEFT JOIN external_player_datacron_actuel pda ON pda.id_datacron = pdaa.id_datacron
            WHERE pdaa.player_id = :playerId AND pdaa.ordre IN (3,6,9,12,15) AND pdaa.ability_id IS NOT NULL
            GROUP BY pda.player_id, pda.id_datacron, pda.set_id, pdaa.ordre, pdaa.ability_id
            """, nativeQuery = true)
    List<PlayerDatacronMecaniqueCheckProjection> findMecaniquesEquipeesParJoueur(@Param("playerId") String playerId);

    @Query(value = """
            SELECT pda.player_id AS "playerId", pda.id_datacron AS "idDatacron", pda.set_id AS "setId",
                   pdaa.stat_type AS "statType", SUM(ROUND(cast(pdaa.stat_value as numeric)/1000000,2)) AS "value"
            FROM external_player_datacron_affix_actuel pdaa
            LEFT JOIN external_player_datacron_actuel pda ON pda.id_datacron = pdaa.id_datacron
            WHERE pdaa.player_id = :playerId AND pdaa.stat_type IS NOT NULL
            GROUP BY pda.player_id, pda.id_datacron, pda.set_id, pdaa.stat_type
            """, nativeQuery = true)
    List<PlayerDatacronStatSumProjection> findSommeStatsParJoueur(@Param("playerId") String playerId);
    
    @Query(value = """
    	    SELECT COUNT(DISTINCT a.id_datacron) FROM external_player_datacron_affix_actuel a
    	    JOIN external_player_datacron_actuel d ON d.id_datacron = a.id_datacron
    	    JOIN external_player p ON p.player_id = d.player_id
    	    WHERE p.guild_id = :guildId AND a.ordre = 9
    	    """, nativeQuery = true)
    	long countDtc9ByGuildId(@Param("guildId") String guildId);
}