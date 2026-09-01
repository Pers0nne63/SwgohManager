package swgohManager.repository;

import swgohManager.model.PlayerDatacronActuel;
import swgohManager.model.PlayerDatacronAffixActuel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerDatacronActuelRepository extends JpaRepository<PlayerDatacronActuel, Long> {
    void deleteByPlayerId(String playerId);
    List<PlayerDatacronActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
}