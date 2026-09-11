package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.OmicronModeSummaryProjection;
import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.ExternalRosterUnitSkillActuel;

public interface ExternalRosterUnitSkillActuelRepository extends JpaRepository<ExternalRosterUnitSkillActuel, Long> {

    void deleteByPlayerId(String playerId);
    List<ExternalRosterUnitSkillActuel> findByPlayerIdAndOmicronAppliedTrue(String playerId);

    @Query(value = """
            SELECT ud.base_id AS baseId, rus.id_skill AS idSkill, rus.omicron_applied AS omicronApplied
            FROM external_roster_unit_skill_actuel rus
            JOIN external_roster_unit_actuel ru ON ru.id_unit = rus.id_unit AND ru.player_id = rus.player_id
            JOIN unit_definition ud ON ud.id_unit = ru.definition_id
            WHERE rus.player_id = :playerId
            """, nativeQuery = true)
    List<PlayerOmicronStatusProjection> findStatutOmicronParJoueur(@Param("playerId") String playerId);
    
    
    @Query(value = """
    	    SELECT sd.omicron_mode AS omicronMode, COUNT(*) AS total
    	    FROM external_roster_unit_skill_actuel rus
    	    JOIN external_player p ON p.player_id = rus.player_id
    	    JOIN skill_definition sd ON sd.id_skill = rus.id_skill
    	    WHERE p.guild_id = :guildId AND rus.omicron_applied = true AND sd.omicron_mode IN :modes
    	    GROUP BY sd.omicron_mode
    	    """, nativeQuery = true)
    	List<OmicronModeSummaryProjection> sommeParModeByGuildId(@Param("guildId") String guildId, @Param("modes") List<String> modes);
}