package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.RosterUnitStatObjectif;

public interface RosterUnitStatObjectifRepository extends JpaRepository<RosterUnitStatObjectif, Long> {
    void deleteByPlayerId(String playerId);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
}