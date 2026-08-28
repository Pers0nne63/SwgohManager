package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.PlayerModQActuel;

public interface PlayerModQActuelRepository extends JpaRepository<PlayerModQActuel, Long> {
    Optional<PlayerModQActuel> findByPlayerId(String playerId);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    List<PlayerModQActuel> findByPlayerIdIn(List<String> playerIds);
}