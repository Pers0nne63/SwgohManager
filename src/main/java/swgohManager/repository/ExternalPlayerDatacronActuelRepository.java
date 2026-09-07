package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import swgohManager.model.ExternalPlayerDatacronActuel;

public interface ExternalPlayerDatacronActuelRepository extends JpaRepository<ExternalPlayerDatacronActuel, Long> {
    void deleteByPlayerId(String playerId);
    List<ExternalPlayerDatacronActuel> findByPlayerId(String playerId);
}