package swgohManager.repository;

import swgohManager.model.FarmPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FarmPlanRepository extends JpaRepository<FarmPlan, Long> {
    List<FarmPlan> findAllByOrderByBaseIdAsc();
}