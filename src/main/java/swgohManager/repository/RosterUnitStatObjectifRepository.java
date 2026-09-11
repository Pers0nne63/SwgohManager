package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.RosterUnitStatObjectif;

public interface RosterUnitStatObjectifRepository extends JpaRepository<RosterUnitStatObjectif, Long> {

    @Modifying
    @Query("DELETE FROM RosterUnitStatObjectif r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);

    @Modifying
    @Query("DELETE FROM RosterUnitStatObjectif r WHERE r.playerId NOT IN :activePlayerIds")
    void deleteByPlayerIdNotIn(@Param("activePlayerIds") List<String> activePlayerIds);
}