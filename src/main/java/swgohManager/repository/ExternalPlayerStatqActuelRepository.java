package swgohManager.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalPlayerStatqActuel;
public interface ExternalPlayerStatqActuelRepository extends JpaRepository<ExternalPlayerStatqActuel, Long> {
    Optional<ExternalPlayerStatqActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = "SELECT AVG(s.statq) FROM external_player_statq_actuel s JOIN external_player p ON p.player_id = s.player_id WHERE p.guild_id = :guildId", nativeQuery = true)
    Double findMoyenneStatqByGuildId(@Param("guildId") String guildId);
}