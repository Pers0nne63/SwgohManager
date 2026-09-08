package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.TbPlanRound;

public interface TbPlanRoundRepository extends JpaRepository<TbPlanRound, Long> {
    List<TbPlanRound> findByPlanIdOrderByRoundNumAsc(Long planId);
    Optional<TbPlanRound> findByPlanIdAndRoundNum(Long planId, Integer roundNum);
    void deleteByPlanId(Long planId);
}