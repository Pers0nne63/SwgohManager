package swgohManager.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.PlayerEraUnitStatusActuel;

public interface PlayerEraUnitStatusActuelRepository extends JpaRepository<PlayerEraUnitStatusActuel, Long> {
    void deleteByPlayerId(String playerId);
    List<PlayerEraUnitStatusActuel> findByPlayerIdIn(List<String> playerIds);
    void deleteByPlayerIdNotIn(List<String> activePlayerIds);
    
    @Query(value = """
        SELECT e.player_id AS playerId,
    		   j.player_name AS playerName,
               e.unit_base_id  AS unitBaseId,
               u.libelle as libelle,
               e.era_level  AS eraLevel,
               r.etoiles AS rarity
        FROM player_era_unit_status_actuel e
        LEFT JOIN joueurs j ON e.player_id  = j.player_id
        LEFT JOIN roster_unit_actuel r ON r.player_id = e.player_id AND SPLIT_PART(r.definition_id,':',1)  = e.unit_base_id
        LEFT JOIN unit_definition u ON r.definition_id = u.id_unit 
        ORDER BY j.player_name
        """, nativeQuery = true)
    List<EraUnitPlayerProjection> findEraUnitsWithRosterData();
}