package swgohManager.repository;
import swgohManager.model.ExternalRosterUnitStatActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExternalRosterUnitStatActuelRepository extends JpaRepository<ExternalRosterUnitStatActuel, Long> {
    List<ExternalRosterUnitStatActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}