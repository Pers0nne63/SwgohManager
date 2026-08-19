package swgohManager.repository;

import swgohManager.model.RosterUnitModActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RosterUnitModActuelRepository extends JpaRepository<RosterUnitModActuel, Long> {
    List<RosterUnitModActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    List<RosterUnitModActuel> findByPlayerIdAndIdSecondaire(String playerId, Integer idSecondaire);
    
}