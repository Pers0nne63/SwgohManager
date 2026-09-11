package swgohManager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalPlayerModQActuel;
import swgohManager.repository.PlayerModQActuelRepository.GuildModqAggregateProjection;

public interface ExternalPlayerModQActuelRepository extends JpaRepository<ExternalPlayerModQActuel, Long> {
    Optional<ExternalPlayerModQActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT AVG(m.modq) AS modQMoyen, COALESCE(SUM(m.mod25plus),0) AS mod25Plus,
    	           COALESCE(SUM(m.mod20_24),0) AS mod20A24, COALESCE(SUM(m.mod15_19),0) AS mod15A19
    	    FROM external_player_modq_actuel m
    	    JOIN external_player p ON p.player_id = m.player_id
    	    WHERE p.guild_id = :guildId
    	    """, nativeQuery = true)
    	GuildModqAggregateProjection findAggregatByGuildId(@Param("guildId") String guildId);
}