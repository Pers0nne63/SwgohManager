package swgohManager.repository;

import swgohManager.model.TbPlaneteReference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TbPlaneteReferenceRepository extends JpaRepository<TbPlaneteReference, Long> {
    List<TbPlaneteReference> findByConflictAndBonus(Integer conflict, Boolean bonus);
    List<TbPlaneteReference> findByPlaneteNameIn(List<String> noms);
}