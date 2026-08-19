package swgohManager.repository;

import swgohManager.model.TerritoryBattle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TerritoryBattleRepository extends JpaRepository<TerritoryBattle, Long> {
    Optional<TerritoryBattle> findByInstanceId(String instanceId);
    Optional<TerritoryBattle> findTopByOrderByEndTimeDesc();
    
}