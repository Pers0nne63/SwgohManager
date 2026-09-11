package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.ExternalRosterUnitModActuel;

public interface ExternalRosterUnitModActuelRepository extends JpaRepository<ExternalRosterUnitModActuel, Long> {
    List<ExternalRosterUnitModActuel> findByPlayerId(String playerId);
    
    List<ExternalRosterUnitModActuel> findByPlayerIdAndIdSecondaire(String playerId, Integer idSecondaire);

    @Modifying
    @Query("DELETE FROM ExternalRosterUnitModActuel r WHERE r.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);

    
    @Query("SELECT COUNT(DISTINCT m.idMod) FROM ExternalRosterUnitModActuel m WHERE m.playerId = :playerId AND m.rarity = :rarity")
    long countDistinctModsByPlayerIdAndRarity(@org.springframework.data.repository.query.Param("playerId") String playerId,
                                               @org.springframework.data.repository.query.Param("rarity") String rarity);
}