package swgohManager.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.RaidHistorique;

public interface RaidHistoriqueRepository extends JpaRepository<RaidHistorique, Long> {

    boolean existsByGuildIdAndPlayerIdAndEndTime(String guildId, String playerId, Instant endTime);
    Optional<RaidHistorique> findTopByOrderByEndTimeDesc();
    List<RaidHistorique> findByEndTime(Instant endTime);
    
    @Query(value = """
            SELECT * FROM (
                SELECT * FROM raid_historique 
                WHERE player_id = :playerId 
                ORDER BY end_time DESC 
                LIMIT 10
            ) sub
            ORDER BY end_time ASC
            """, nativeQuery = true)
        List<RaidHistorique> findTop10ByPlayerIdOrderByEndTimeAsc(@Param("playerId") String playerId);
}