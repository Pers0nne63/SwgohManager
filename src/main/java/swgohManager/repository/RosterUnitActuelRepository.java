package swgohManager.repository;

import swgohManager.model.RosterUnitActuel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swgohManager.controller.dto.RosterBaseIdProgressProjection;

public interface RosterUnitActuelRepository extends JpaRepository<RosterUnitActuel, Long> {
    List<RosterUnitActuel> findByPlayerId(String playerId);
    void deleteByPlayerId(String playerId);
    
    @Query(value = """
    	    SELECT ud.base_id AS baseId, MAX(ru.etoiles) AS maxEtoiles, MAX(ru.relic) AS maxRelic
    	    FROM roster_unit_actuel ru
    	    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
    	    WHERE ru.player_id = :playerId
    	    GROUP BY ud.base_id
    	    """, nativeQuery = true)
    	List<RosterBaseIdProgressProjection> findMaxEtoilesRelicByBaseId(@Param("playerId") String playerId);
}