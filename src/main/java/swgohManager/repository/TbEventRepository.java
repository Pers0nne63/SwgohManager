package swgohManager.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.TbEvent;

public interface TbEventRepository extends JpaRepository<TbEvent, Long> {

    /** Ids d'événements déjà en base pour une TB : sert à n'insérer que les nouveaux. */
    @Query("SELECT e.eventId FROM TbEvent e WHERE e.instanceId = :instanceId")
    Set<String> findEventIdsByInstanceId(@Param("instanceId") String instanceId);
}