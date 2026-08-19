package swgohManager.repository;

import swgohManager.model.SyncExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncExecutionRepository extends JpaRepository<SyncExecution, Long> {}