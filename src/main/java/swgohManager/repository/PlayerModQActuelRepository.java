package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.PlayerModQActuel;

public interface PlayerModQActuelRepository extends JpaRepository<PlayerModQActuel, Long> {
    
	public interface GuildModqAggregateProjection {
	    Double getModQMoyen();
	    Long getMod25Plus();
	    Long getMod20A24();
	    Long getMod15A19();
	}
	
	Optional<PlayerModQActuel> findByPlayerId(String playerId);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    List<PlayerModQActuel> findByPlayerIdIn(List<String> playerIds);
    
   @Query(value = """
        SELECT AVG(m.modq) AS "modQMoyen", COALESCE(SUM(m.mod25plus),0) AS "mod25Plus",
               COALESCE(SUM(m.mod20_24),0) AS "mod20A24", COALESCE(SUM(m.mod15_19),0) AS "mod15A19"
        FROM player_modq_actuel m
        JOIN joueurs j ON j.player_id = m.player_id
        WHERE j.present_in_guild = true
        """, nativeQuery = true)
    GuildModqAggregateProjection findAggregatGuilde();
}

