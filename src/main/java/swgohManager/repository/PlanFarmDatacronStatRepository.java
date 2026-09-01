package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.PlanFarmDatacronStat;

public interface PlanFarmDatacronStatRepository extends JpaRepository<PlanFarmDatacronStat, Long> {
    List<PlanFarmDatacronStat> findByPlanFarmDatacronId(Long planFarmDatacronId);
    void deleteByPlanFarmDatacronId(Long planFarmDatacronId);
}