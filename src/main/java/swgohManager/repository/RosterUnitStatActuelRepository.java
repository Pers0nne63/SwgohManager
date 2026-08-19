package swgohManager.repository;

import swgohManager.model.RosterUnitStatActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RosterUnitStatActuelRepository extends JpaRepository<RosterUnitStatActuel, Long> {
    List<RosterUnitStatActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}