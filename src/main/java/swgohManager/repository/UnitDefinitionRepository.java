package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.BaseIdLibelleProjection;
import swgohManager.model.UnitDefinition;

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
    
    @Query("SELECT DISTINCT u.baseId AS baseId, u.libelle AS libelle FROM UnitDefinition u WHERE u.legend = true AND u.baseId IS NOT NULL")
    List<BaseIdLibelleProjection> findDistinctLegendBaseIdsAvecLibelle();

    @Query("SELECT DISTINCT u.baseId AS baseId, u.libelle AS libelle FROM UnitDefinition u WHERE u.conquete = true AND u.baseId IS NOT NULL")
    List<BaseIdLibelleProjection> findDistinctConqueteBaseIdsAvecLibelle();
    
    @Modifying
    @Query("DELETE FROM UnitDefinition u WHERE u.idUnit NOT IN :idsUnitsRecus")
    void deleteByIdUnitNotIn(@Param("idsUnitsRecus") List<String> idsUnitsRecus);
}