package swgohManager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.ExternalPlayerEraUnitStatusActuel;

public interface ExternalPlayerEraUnitStatusActuelRepository extends JpaRepository<ExternalPlayerEraUnitStatusActuel, Long> {
    void deleteByPlayerId(String playerId);
}