package swgohManager.repository;

import swgohManager.model.LeaderboardMod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardModRepository extends JpaRepository<LeaderboardMod, Long> {
    void deleteByPlayerId(String playerId);
}