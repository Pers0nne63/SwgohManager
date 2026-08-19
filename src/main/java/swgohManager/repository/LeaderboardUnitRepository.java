package swgohManager.repository;

import swgohManager.model.LeaderboardUnit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardUnitRepository extends JpaRepository<LeaderboardUnit, Long> {
    void deleteByPlayerId(String playerId);
}