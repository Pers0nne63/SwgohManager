package swgohManager.repository;

import swgohManager.model.ExternalPlayerModQActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalPlayerModQActuelRepository extends JpaRepository<ExternalPlayerModQActuel, Long> {
    Optional<ExternalPlayerModQActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}