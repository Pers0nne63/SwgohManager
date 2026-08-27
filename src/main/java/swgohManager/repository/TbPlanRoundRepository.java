package swgohManager.repository;

import swgohManager.model.TbPlanRound;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TbPlanRoundRepository extends JpaRepository<TbPlanRound, Long> {
    List<TbPlanRound> findByPlanIdOrderByRoundNumAsc(Long planId);
    void deleteByPlanId(Long planId);
}