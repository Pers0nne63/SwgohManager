package swgohManager.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.ExternalPlayerRatingActuel;

public interface ExternalPlayerRatingActuelRepository extends JpaRepository<ExternalPlayerRatingActuel, Long> {
    Optional<ExternalPlayerRatingActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}