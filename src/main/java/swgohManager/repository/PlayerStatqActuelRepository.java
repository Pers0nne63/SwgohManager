package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.PlayerStatqActuel;

public interface PlayerStatqActuelRepository extends JpaRepository<PlayerStatqActuel, Long> {
    Optional<PlayerStatqActuel> findByPlayerId(String playerId);
    List<PlayerStatqActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    
    @Query("SELECT AVG(p.statq) FROM PlayerStatqActuel p JOIN Joueur j ON j.playerId = p.playerId WHERE j.presentInGuild = true")
    Double findMoyenneStatqGuilde();
}