package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.TerritoryBattle;

public interface TerritoryBattleRepository extends JpaRepository<TerritoryBattle, Long> {
    Optional<TerritoryBattle> findByInstanceId(String instanceId);
    Optional<TerritoryBattle> findTopByOrderByEndTimeDesc();
    List<TerritoryBattle> findAllByOrderByStartTimeDesc();
}