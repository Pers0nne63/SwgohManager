package swgohManager.repository;

import swgohManager.controller.dto.OmicronModeSummaryProjection;
import swgohManager.model.PlayerOmicronModeActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerOmicronModeActuelRepository extends JpaRepository<PlayerOmicronModeActuel, Long> {

    @Modifying
    @Query("DELETE FROM PlayerOmicronModeActuel p WHERE p.playerId = :playerId")
    void deleteByPlayerId(String playerId);

    void deleteByPlayerIdNotIn(List<String> activePlayerIds);

    @Query("""
        SELECT p.omicronMode AS omicronMode, SUM(p.nbOmicron) AS total
        FROM PlayerOmicronModeActuel p
        GROUP BY p.omicronMode
        """)
    List<OmicronModeSummaryProjection> sommeParMode();
}