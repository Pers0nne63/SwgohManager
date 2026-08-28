package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swgohManager.model.UnitDefinition;
import swgohManager.controller.dto.BaseIdLibelleProjection;

import java.util.List;

public interface UnitDefinitionRepository extends JpaRepository<UnitDefinition, Long> {

    @Query("SELECT DISTINCT u.baseId FROM UnitDefinition u WHERE u.baseId IS NOT NULL ORDER BY u.baseId")
    List<String> findDistinctBaseIds();
    List<UnitDefinition> findByIdUnitIn(List<String> idUnits);
    
    @Query("SELECT DISTINCT u.baseId AS baseId, u.libelle AS libelle FROM UnitDefinition u WHERE u.baseId IS NOT NULL")
    List<BaseIdLibelleProjection> findDistinctBaseIdsAvecLibelle();
    
    @Query(value = """
    SELECT DISTINCT u.base_id AS baseId, u.libelle AS libelle
    FROM roster_unit_actuel rua
    LEFT JOIN  unit_definition u ON u.id_unit=rua.definition_id
    WHERE rua.relic IS NOT NULL
    """, nativeQuery = true)
    List<BaseIdLibelleProjection> findDistinctPlayableBaseIdsAvecLibelle();
}