package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import swgohManager.model.KyberStatSnapshot;

@Repository
public interface KyberStatSnapshotRepository extends JpaRepository<KyberStatSnapshot, Long> {

    KyberStatSnapshot findFirstByOrderByDateSnapshotDesc();

    List<KyberStatSnapshot> findTop26ByOrderByDateSnapshotDesc();
}