package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.DatacronTemplate;

public interface DatacronTemplateRepository extends JpaRepository<DatacronTemplate, Long> {
    void deleteAllInBatch();
}