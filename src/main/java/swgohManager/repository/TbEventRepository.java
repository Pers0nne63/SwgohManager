package swgohManager.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.TbEvent;

public interface TbEventRepository extends JpaRepository<TbEvent, Long> {

    /** Trou entre deux événements consécutifs (avec points) d'une même zone. */
    interface TrouProjection {
        String getEventId();          // événement situé APRÈS le trou
        String getZoneId();
        Long getDebutMs();            // timestamp (ms) de l'événement précédent connu
        Long getFinMs();              // timestamp (ms) de l'événement suivant connu
        Long getPointsManquants();
    }

    /** Ids d'événements déjà en base pour une TB : sert à n'insérer que les nouveaux. */
    @Query("SELECT e.eventId FROM TbEvent e WHERE e.instanceId = :instanceId")
    Set<String> findEventIdsByInstanceId(@Param("instanceId") String instanceId);

    /**
     * Trous de la TB : pour chaque zone, les événements portant des points (score_total > 0) sont chaînés par date ;
     * le score cumulé doit augmenter exactement de score_delta d'un événement au suivant, sinon la différence
     * correspond aux points d'événements jamais reçus (hors de la fenêtre de 50 événements de l'API).
     * Ne détecte pas les covert / étoiles manquants (pas de points). Le 1er événement connu d'une zone n'a pas de précédent.
     */
    @Query(value = """
            SELECT t."eventId" AS "eventId", t."zoneId" AS "zoneId", t."debutMs" AS "debutMs",
                   t."finMs" AS "finMs", t."pointsManquants" AS "pointsManquants"
            FROM (
                SELECT e.event_id AS "eventId",
                       e.zone_id AS "zoneId",
                       CAST(EXTRACT(EPOCH FROM LAG(e.event_time) OVER w) * 1000 AS bigint) AS "debutMs",
                       CAST(EXTRACT(EPOCH FROM e.event_time) * 1000 AS bigint) AS "finMs",
                       e.score_total - LAG(e.score_total) OVER w - e.score_delta AS "pointsManquants"
                FROM tb_event e
                WHERE e.instance_id = :instanceId AND e.score_total > 0
                WINDOW w AS (PARTITION BY e.zone_id ORDER BY e.event_time, e.id)
            ) t
            WHERE t."pointsManquants" IS NOT NULL AND t."pointsManquants" <> 0
            ORDER BY t."zoneId", t."finMs"
            """, nativeQuery = true)
    List<TrouProjection> findTrousByInstanceId(@Param("instanceId") String instanceId);
}