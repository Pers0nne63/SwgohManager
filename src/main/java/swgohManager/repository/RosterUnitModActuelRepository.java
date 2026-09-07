package swgohManager.repository;

import swgohManager.model.RosterUnitModActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RosterUnitModActuelRepository extends JpaRepository<RosterUnitModActuel, Long> {
    List<RosterUnitModActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    List<RosterUnitModActuel> findByPlayerIdAndIdSecondaire(String playerId, Integer idSecondaire);

    // 👇 remplace countByPlayerIdAndRarity : compte les mods distincts (pas les lignes stat secondaire)
    @Query("SELECT COUNT(DISTINCT m.idMod) FROM RosterUnitModActuel m WHERE m.playerId = :playerId AND m.rarity = :rarity")
    long countDistinctModsByPlayerIdAndRarity(@Param("playerId") String playerId, @Param("rarity") String rarity);
}