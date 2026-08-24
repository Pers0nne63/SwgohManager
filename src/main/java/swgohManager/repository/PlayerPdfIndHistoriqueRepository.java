package swgohManager.repository;

import swgohManager.model.PlayerPdfIndHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerPdfIndHistoriqueRepository extends JpaRepository<PlayerPdfIndHistorique, Long> {
    List<PlayerPdfIndHistorique> findByPlayerIdOrderByIdSyncAsc(String playerId);
}