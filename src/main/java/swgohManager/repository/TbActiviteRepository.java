package swgohManager.repository;

import swgohManager.model.TbActivite;
import swgohManager.model.TerritoryBattle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TbActiviteRepository extends JpaRepository<TbActivite, Long> {
    List<TbActivite> findByTerritoryBattle(TerritoryBattle territoryBattle);
}