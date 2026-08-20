package swgohManager.repository;

import swgohManager.model.PlayerStatqDetailActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerStatqDetailActuelRepository extends JpaRepository<PlayerStatqDetailActuel, Long> {
    List<PlayerStatqDetailActuel> findByPlayerId(String playerId);
}