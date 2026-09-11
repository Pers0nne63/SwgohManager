package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalPlayerDatacronActuel;

public interface ExternalPlayerDatacronActuelRepository extends JpaRepository<ExternalPlayerDatacronActuel, Long> {
    void deleteByPlayerId(String playerId);
    List<ExternalPlayerDatacronActuel> findByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT COUNT(*) FROM external_player_datacron_actuel d
    	    JOIN external_player p ON p.player_id = d.player_id
    	    WHERE p.guild_id = :guildId AND d.focused = true
    	    """, nativeQuery = true)
    	long countFdtcByGuildId(@Param("guildId") String guildId);
}