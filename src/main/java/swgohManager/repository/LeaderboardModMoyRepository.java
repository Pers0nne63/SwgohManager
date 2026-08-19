package swgohManager.repository;

import swgohManager.model.LeaderboardModMoy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeaderboardModMoyRepository extends JpaRepository<LeaderboardModMoy, Long> {
    Optional<LeaderboardModMoy> findByBaseId(String baseId);
}