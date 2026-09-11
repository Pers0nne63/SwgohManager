package swgohManager.repository;

import swgohManager.model.RosterUnitStatActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RosterUnitStatActuelRepository extends JpaRepository<RosterUnitStatActuel, Long> {
    List<RosterUnitStatActuel> findByPlayerId(String playerId);

    @Modifying
    @Query("DELETE FROM RosterUnitStatActuel r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);

    @Modifying
    @Query("DELETE FROM RosterUnitStatActuel r WHERE r.playerId NOT IN :activePlayerIds")
    void deleteByPlayerIdNotIn(@Param("activePlayerIds") List<String> activePlayerIds);
}