package swgohManager.repository;
import swgohManager.model.ExternalRosterUnitStatObjectif;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExternalRosterUnitStatObjectifRepository extends JpaRepository<ExternalRosterUnitStatObjectif, Long> {
    List<ExternalRosterUnitStatObjectif> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}