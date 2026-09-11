package swgohManager.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalPlayer;

public interface ExternalPlayerRepository extends JpaRepository<ExternalPlayer, Long> {
    Optional<ExternalPlayer> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    List<ExternalPlayer> findAllByOrderByPlayerNameAsc(); 

    @Query("SELECT e.playerId FROM ExternalPlayer e WHERE e.dateScan < :seuil")
    List<String> findPlayerIdsScannesAvant(Instant seuil);
    
    @Query(value = "SELECT SUM(galactic_power) FROM external_player WHERE guild_id = :guildId", nativeQuery = true)
    Long sumGalacticPowerByGuildId(@Param("guildId") String guildId);
    
    @Query("SELECT p.id FROM ExternalPlayer p WHERE p.guildId = :guildId")
    List<String> findPlayerIdsByGuildId(@Param("guildId") String guildId);
   
}