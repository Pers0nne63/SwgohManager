package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.TbImportActivite;
import swgohManager.model.TerritoryBattle;

import java.util.Optional;

public interface TbImportActiviteRepository extends JpaRepository<TbImportActivite, Long> {
    Optional<TbImportActivite> findByTerritoryBattleAndMapStatId(TerritoryBattle territoryBattle, String mapStatId);
    Optional<TbImportActivite> findByTerritoryBattleAndMapStatIdAndRoundNum(
            TerritoryBattle territoryBattle, String mapStatId, Integer roundNum);
}