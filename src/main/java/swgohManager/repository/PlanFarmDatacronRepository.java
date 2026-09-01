package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.PlanFarmDatacron;

public interface PlanFarmDatacronRepository extends JpaRepository<PlanFarmDatacron, Long> {
    List<PlanFarmDatacron> findBySetId(String setId);
    void deleteBySetId(String setId);
}