package swgohManager.repository;

import swgohManager.model.PlayerPdfHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerPdfHistoriqueRepository extends JpaRepository<PlayerPdfHistorique, Long> {
    List<PlayerPdfHistorique> findByPlayerIdOrderByIdSyncAsc(String playerId);
}