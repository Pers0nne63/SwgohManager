package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.StatProgression;

public interface StatProgressionRepository extends JpaRepository<StatProgression, Long> {
	List<StatProgression> findByStatProgressionIdIn(List<String> ids);
	void deleteByPlayerIdNotIn(List<String> activePlayerIds);
}