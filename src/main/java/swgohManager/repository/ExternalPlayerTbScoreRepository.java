package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalPlayerTbScore;

public interface ExternalPlayerTbScoreRepository extends JpaRepository<ExternalPlayerTbScore, Long> {
    List<ExternalPlayerTbScore> findByPlayerId(String playerId);

    @Modifying
    @Query("DELETE FROM ExternalPlayerTbScore t WHERE t.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);
}