package swgohManager.repository;
import swgohManager.model.ExternalRosterUnitStatObjectif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface ExternalRosterUnitStatObjectifRepository extends JpaRepository<ExternalRosterUnitStatObjectif, Long> {
    List<ExternalRosterUnitStatObjectif> findByPlayerId(String playerId);
    
    @Modifying
    @Query("DELETE FROM ExternalRosterUnitStatObjectif r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);
}