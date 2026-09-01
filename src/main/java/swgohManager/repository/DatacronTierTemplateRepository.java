package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.DatacronTierTemplate;

public interface DatacronTierTemplateRepository extends JpaRepository<DatacronTierTemplate, Long> {
    void deleteAllInBatch();
}