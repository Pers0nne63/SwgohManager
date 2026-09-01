package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.AbilityDefinition;

public interface AbilityDefinitionRepository extends JpaRepository<AbilityDefinition, String> {
    void deleteAllInBatch();
}