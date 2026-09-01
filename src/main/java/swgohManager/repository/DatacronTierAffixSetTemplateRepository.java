package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.DatacronTierAffixSetTemplate;

public interface DatacronTierAffixSetTemplateRepository extends JpaRepository<DatacronTierAffixSetTemplate, Long> {
    void deleteAllInBatch();
}