package swgohManager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swgohManager.model.RosterUnitHistorique;

@Repository
public interface RosterUnitHistoriqueRepository extends JpaRepository<RosterUnitHistorique, Long> {

    @Modifying
    @Query(value = "INSERT INTO roster_unit_historique (player_id, id_unit, definition_id, etoiles, niveau, gear, relic, id_sync) " +
                   "SELECT player_id, id_unit, definition_id, etoiles, niveau, gear, relic, id_sync FROM roster_unit_actuel", nativeQuery = true)
    int copierRosterActuelVersHistorique();
}