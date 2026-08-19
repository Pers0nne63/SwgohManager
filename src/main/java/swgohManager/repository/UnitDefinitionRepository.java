package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import swgohManager.model.UnitDefinition;

import java.util.List;

public interface UnitDefinitionRepository extends JpaRepository<UnitDefinition, Long> {

    @Query("SELECT DISTINCT u.baseId FROM UnitDefinition u WHERE u.baseId IS NOT NULL ORDER BY u.baseId")
    List<String> findDistinctBaseIds();
    List<UnitDefinition> findByIdUnitIn(List<String> idUnits);
}