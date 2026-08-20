package swgohManager.repository;

import swgohManager.model.PlayerStatqActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerStatqActuelRepository extends JpaRepository<PlayerStatqActuel, Long> {
    Optional<PlayerStatqActuel> findByPlayerId(String playerId);
    List<PlayerStatqActuel> findByPlayerIdIn(List<String> playerIds);
}