package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}