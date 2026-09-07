package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.ExternalRosterUnitModActuel;

public interface ExternalRosterUnitModActuelRepository extends JpaRepository<ExternalRosterUnitModActuel, Long> {
    List<ExternalRosterUnitModActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    List<ExternalRosterUnitModActuel> findByPlayerIdAndIdSecondaire(String playerId, Integer idSecondaire);

    @Query("SELECT COUNT(DISTINCT m.idMod) FROM ExternalRosterUnitModActuel m WHERE m.playerId = :playerId AND m.rarity = :rarity")
    long countDistinctModsByPlayerIdAndRarity(@org.springframework.data.repository.query.Param("playerId") String playerId,
                                               @org.springframework.data.repository.query.Param("rarity") String rarity);
}