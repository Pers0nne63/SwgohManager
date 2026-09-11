package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.PlayerDatacronActuel;

public interface PlayerDatacronActuelRepository extends JpaRepository<PlayerDatacronActuel, Long> {
    void deleteByPlayerId(String playerId);
    List<PlayerDatacronActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    
    @Query(value = """
    	    SELECT COUNT(*) FROM player_datacron_actuel d
    	    JOIN joueurs j ON j.player_id = d.player_id
    	    WHERE j.present_in_guild = true AND d.focused = true
    	    """, nativeQuery = true)
    	long countFdtcGuilde();
}