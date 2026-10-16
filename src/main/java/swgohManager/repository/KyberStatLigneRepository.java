package swgohManager.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import swgohManager.model.KyberStatLigne;

@Repository
public interface KyberStatLigneRepository extends JpaRepository<KyberStatLigne, Long> {

    List<KyberStatLigne> findBySnapshotIdOrderByTypeAscCleAsc(Long snapshotId);
    List<KyberStatLigne> findBySnapshotIdInAndType(Collection<Long> snapshotIds, String type);
}