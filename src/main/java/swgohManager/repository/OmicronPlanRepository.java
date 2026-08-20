package swgohManager.repository;

import swgohManager.model.OmicronPlan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OmicronPlanRepository extends JpaRepository<OmicronPlan, Long> {
    List<OmicronPlan> findAllByOrderByPrioriteAscBaseIdAsc();
    
}