package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.controller.dto.ExternalEraUnitProjection;
import swgohManager.model.ExternalPlayerEraUnitStatusActuel;

public interface ExternalPlayerEraUnitStatusActuelRepository extends JpaRepository<ExternalPlayerEraUnitStatusActuel, Long> {

    void deleteByPlayerId(String playerId);

    @Query(value = """
        SELECT e.unit_base_id AS unitBaseId,
               u.libelle      AS libelle,
               e.era_level    AS eraLevel,
               r.etoiles      AS rarity
        FROM external_player_era_unit_status_actuel e
        LEFT JOIN external_roster_unit_actuel r
               ON r.player_id = e.player_id
              AND SPLIT_PART(r.definition_id, ':', 1) = e.unit_base_id
        LEFT JOIN unit_definition u ON r.definition_id = u.id_unit
        WHERE e.player_id = :playerId
        ORDER BY e.era_level DESC, u.libelle
        """, nativeQuery = true)
    List<ExternalEraUnitProjection> findEraUnitsByPlayerId(@Param("playerId") String playerId);
}