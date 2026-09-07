package swgohManager.repository;

import swgohManager.model.ExternalPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExternalPlayerRepository extends JpaRepository<ExternalPlayer, Long> {
    Optional<ExternalPlayer> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    List<ExternalPlayer> findAllByOrderByPlayerNameAsc(); 

    @Query("SELECT e.playerId FROM ExternalPlayer e WHERE e.dateScan < :seuil")
    List<String> findPlayerIdsScannesAvant(Instant seuil);
}