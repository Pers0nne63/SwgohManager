package swgohManager.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.UnitTierDefinition;
public interface UnitTierDefinitionRepository extends JpaRepository<UnitTierDefinition, Long> {
	List<UnitTierDefinition> findByIdUnitIn(List<String> idUnits);
}