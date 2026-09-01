package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.BattleTargetingRuleCategory;

public interface BattleTargetingRuleCategoryRepository extends JpaRepository<BattleTargetingRuleCategory, Long> {
    void deleteAllInBatch();
}