package swgohManager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.LeaderboardModMoy;

public interface LeaderboardModMoyRepository extends JpaRepository<LeaderboardModMoy, Long> {
    Optional<LeaderboardModMoy> findByBaseId(String baseId);

}