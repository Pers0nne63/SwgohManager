package swgohManager.repository;

import swgohManager.model.ExternalPlayerRaid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalPlayerRaidRepository extends JpaRepository<ExternalPlayerRaid, Long> {
    Optional<ExternalPlayerRaid> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}