package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swgohManager.model.DatacronAffixTemplate;

public interface DatacronAffixTemplateRepository extends JpaRepository<DatacronAffixTemplate, Long> {
    void deleteAllInBatch();
}