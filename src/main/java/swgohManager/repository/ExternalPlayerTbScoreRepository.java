package swgohManager.repository;

import swgohManager.model.ExternalPlayerTbScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExternalPlayerTbScoreRepository extends JpaRepository<ExternalPlayerTbScore, Long> {
    List<ExternalPlayerTbScore> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
}