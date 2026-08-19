package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.UnitStatPriority;
import java.util.Optional;

public interface UnitStatPriorityRepository extends JpaRepository<UnitStatPriority, Long> {
    Optional<UnitStatPriority> findByBaseId(String baseId);
}