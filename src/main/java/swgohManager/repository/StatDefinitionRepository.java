package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.StatDefinition;
import java.util.List;

public interface StatDefinitionRepository extends JpaRepository<StatDefinition, Integer> {

    // 👈 Récupère uniquement les stats où statq = true
    List<StatDefinition> findByIsStatqTrue();
}