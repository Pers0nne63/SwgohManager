package swgohManager.repository;

import swgohManager.model.PlayerPdfOmicronHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerPdfOmicronHistoriqueRepository extends JpaRepository<PlayerPdfOmicronHistorique, Long> {
    List<PlayerPdfOmicronHistorique> findByPlayerIdOrderByIdSyncAsc(String playerId);
}