package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swgohManager.model.PlanFarmInd;
import java.util.List;

@Repository
public interface PlanFarmIndRepository extends JpaRepository<PlanFarmInd, Long> {
    List<PlanFarmInd> findByPlayerId(String playerId);
    
    boolean existsByPlayerId(String playerId);
}