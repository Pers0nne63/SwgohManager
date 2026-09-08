package swgohManager.repository;
import swgohManager.model.ExternalPlayerStatqActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ExternalPlayerStatqActuelRepository extends JpaRepository<ExternalPlayerStatqActuel, Long> {
    Optional<ExternalPlayerStatqActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}