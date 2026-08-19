package swgohManager.repository;

import swgohManager.model.RosterUnitSkillActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swgohManager.controller.dto.OmicronOptionProjection;
import swgohManager.controller.dto.PlayerOmicronStatusProjection;

public interface RosterUnitSkillActuelRepository extends JpaRepository<RosterUnitSkillActuel, Long> {
    List<RosterUnitSkillActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT DISTINCT ud.base_id AS baseId, rus.id_skill AS idSkill, rus.type AS type, rus.numero AS numero
    	    FROM roster_unit_skill_actuel rus
    	    JOIN roster_unit_actuel ru ON ru.id_unit = rus.id_unit AND ru.player_id = rus.player_id
    	    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
    	    JOIN skill_definition sd ON sd.id_skill = rus.id_skill
    	    WHERE sd.skill_omicron = true
    	    ORDER BY ud.base_id, rus.type, rus.numero
    	    """, nativeQuery = true)
    	List<OmicronOptionProjection> findOptionsOmicron();

    	@Query(value = """
    	    SELECT ud.base_id AS baseId, rus.id_skill AS idSkill, rus.omicron_applied AS omicronApplied
    	    FROM roster_unit_skill_actuel rus
    	    JOIN roster_unit_actuel ru ON ru.id_unit = rus.id_unit AND ru.player_id = rus.player_id
    	    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
    	    WHERE rus.player_id = :playerId
    	    """, nativeQuery = true)
    	List<PlayerOmicronStatusProjection> findStatutOmicronParJoueur(@Param("playerId") String playerId);
}