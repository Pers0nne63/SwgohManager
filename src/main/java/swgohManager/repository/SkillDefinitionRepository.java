package swgohManager.repository;

import swgohManager.model.SkillDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SkillDefinitionRepository extends JpaRepository<SkillDefinition, Long> {
    Optional<SkillDefinition> findByIdSkill(String idSkill);
}