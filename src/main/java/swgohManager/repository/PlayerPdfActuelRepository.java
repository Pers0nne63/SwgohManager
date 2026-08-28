package swgohManager.repository;

import swgohManager.model.PlayerPdfActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerPdfActuelRepository extends JpaRepository<PlayerPdfActuel, Long> {
    Optional<PlayerPdfActuel> findByPlayerId(String playerId);
    List<PlayerPdfActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
}