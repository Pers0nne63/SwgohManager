package swgohManager.repository;

import swgohManager.model.PlayerPdfOmicronActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerPdfOmicronActuelRepository extends JpaRepository<PlayerPdfOmicronActuel, Long> {
    Optional<PlayerPdfOmicronActuel> findByPlayerId(String playerId);
    List<PlayerPdfOmicronActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    Optional<PlayerPdfOmicronActuel> findByPlayerIdAndPriorite(String playerId, String priorite);
}