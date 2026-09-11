package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.RosterUnitModActuel;

public interface RosterUnitModActuelRepository extends JpaRepository<RosterUnitModActuel, Long> {
    List<RosterUnitModActuel> findByPlayerId(String playerId);
    @Modifying
    @Query("DELETE FROM RosterUnitModActuel m WHERE m.playerId = :playerId")
    void deleteByPlayerId(@Param("playerId") String playerId);

    @Modifying
    @Query("DELETE FROM RosterUnitModActuel m WHERE m.playerId NOT IN :activePlayerIds")
    void deleteByPlayerIdNotIn(@Param("activePlayerIds") List<String> activePlayerIds);
    List<RosterUnitModActuel> findByPlayerIdAndIdSecondaire(String playerId, Integer idSecondaire);

    // 👇 remplace countByPlayerIdAndRarity : compte les mods distincts (pas les lignes stat secondaire)
    @Query("SELECT COUNT(DISTINCT m.idMod) FROM RosterUnitModActuel m WHERE m.playerId = :playerId AND m.rarity = :rarity")
    long countDistinctModsByPlayerIdAndRarity(@Param("playerId") String playerId, @Param("rarity") String rarity);
}