package swgohManager.repository;

import swgohManager.model.RosterUnitStatObjectif;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterUnitStatObjectifRepository extends JpaRepository<RosterUnitStatObjectif, Long> {
    void deleteByPlayerId(String playerId);
}