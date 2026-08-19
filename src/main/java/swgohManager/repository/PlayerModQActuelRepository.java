package swgohManager.repository;

import swgohManager.model.PlayerModQActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerModQActuelRepository extends JpaRepository<PlayerModQActuel, Long> {
    Optional<PlayerModQActuel> findByPlayerId(String playerId);
}