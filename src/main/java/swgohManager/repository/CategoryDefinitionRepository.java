package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.CategoryDefinition;

public interface CategoryDefinitionRepository extends JpaRepository<CategoryDefinition, String> {
    void deleteAllInBatch();
}