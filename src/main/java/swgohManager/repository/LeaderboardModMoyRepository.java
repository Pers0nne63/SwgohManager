package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.LeaderboardMod;
import swgohManager.model.LeaderboardModMoy;

public interface LeaderboardModMoyRepository extends JpaRepository<LeaderboardModMoy, Long> {
    Optional<LeaderboardModMoy> findByBaseId(String baseId);
    
    List<LeaderboardMod> findByPlayerId(String playerId);
}