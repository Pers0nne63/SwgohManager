package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.PlanFarmDatacronMecanique;

public interface PlanFarmDatacronMecaniqueRepository extends JpaRepository<PlanFarmDatacronMecanique, Long> {
    List<PlanFarmDatacronMecanique> findByPlanFarmDatacronId(Long planFarmDatacronId);
    void deleteByPlanFarmDatacronId(Long planFarmDatacronId);
}