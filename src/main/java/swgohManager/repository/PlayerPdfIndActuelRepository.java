package swgohManager.repository;

import swgohManager.model.PlayerPdfIndActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerPdfIndActuelRepository extends JpaRepository<PlayerPdfIndActuel, Long> {
    Optional<PlayerPdfIndActuel> findByPlayerId(String playerId);
    List<PlayerPdfIndActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
}