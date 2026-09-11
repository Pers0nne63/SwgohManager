package swgohManager.repository;
import swgohManager.model.ExternalRosterUnitStatActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface ExternalRosterUnitStatActuelRepository extends JpaRepository<ExternalRosterUnitStatActuel, Long> {
    List<ExternalRosterUnitStatActuel> findByPlayerId(String playerId);
    
    @Modifying
    @Query("DELETE FROM ExternalRosterUnitStatActuel r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);
}