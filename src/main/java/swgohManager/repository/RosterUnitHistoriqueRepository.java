package swgohManager.repository;

import swgohManager.model.RosterUnitHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swgohManager.controller.dto.RosterHistoryProgressProjection;
import java.util.List;

public interface RosterUnitHistoriqueRepository extends JpaRepository<RosterUnitHistorique, Long> {
	@Query(value = """
		    SELECT ru.id_sync AS idSync, ud.base_id AS baseId, MAX(ru.etoiles) AS maxEtoiles, MAX(ru.relic) AS maxRelic
		    FROM (
		        SELECT id_sync, definition_id, etoiles, relic FROM roster_unit_historique WHERE player_id = :playerId
		        UNION ALL
		        SELECT id_sync, definition_id, etoiles, relic FROM roster_unit_actuel WHERE player_id = :playerId
		    ) ru
		    JOIN unit_definition ud ON ud.id_unit = ru.definition_id
		    GROUP BY ru.id_sync, ud.base_id
		    ORDER BY ru.id_sync
		    """, nativeQuery = true)
		List<RosterHistoryProgressProjection> findProgressionParSync(@Param("playerId") String playerId);
}