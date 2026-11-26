package swgohManager.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un événement du canal d'activité d'une zone de Bataille de Territoire (endpoint guionbot tb-events.php).
 * Table unique : toutes les variantes d'événement y sont stockées, les colonnes propres à un type
 * (étoiles, progression de mission...) restent null pour les autres types.
 * Les événements sont immuables : l'import ne fait qu'insérer ceux dont l'id n'existe pas encore.
 */
@Entity
@Table(name = "tb_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_tb_event_event_id", columnNames = "event_id"),
        indexes = {
                @Index(name = "idx_tb_event_instance_zone", columnList = "instance_id, zone_id"),
                @Index(name = "idx_tb_event_author", columnList = "author_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class TbEvent {

    /** Type fonctionnel déduit de la clé de message du jeu. AUTRE = clé inconnue (message_key conserve la valeur brute). */
    public enum Type {
        DEPLOY,           // déploiement en zone de conflit (points = scoreDelta)
        STRIKE_COMPLETE,  // combat (strike) terminé : tous les objectifs liés sont faits
        STRIKE_PARTIAL,   // combat (strike) avec contribution partielle
        COVERT,           // mission spéciale (covert) terminée
        STARS,            // étoile(s) de la zone atteinte(s)
        AUTRE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_event_seq")
    @SequenceGenerator(name = "tb_event_seq", sequenceName = "tb_event_seq", allocationSize = 500)
    private Long id;

    /** Id de l'événement côté jeu (clé naturelle, sert à dédoublonner entre deux imports). */
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    /** Identifiant de la TB (ex. TB_EVENT_TB3_MIXED:O1795456800000), = mapInstanceId du fichier. */
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;

    /** Id de la guilde, extrait du channelId (TZ-{guildId}-...). */
    @Column(name = "guild_id", length = 40)
    private String guildId;

    @Column(name = "map_id", length = 30)
    private String mapId;

    @Column(name = "zone_id", length = 100)
    private String zoneId;

    /** Dérivés de zone_id (…_phase01_conflict02[_bonus]) pour faciliter les requêtes. */
    @Column(name = "phase")
    private Integer phase;

    @Column(name = "conflict")
    private Integer conflict;

    @Column(name = "bonus", nullable = false)
    private boolean bonus;

    @Column(name = "event_type", length = 30)
    private String eventType;

    @Column(name = "activity_type", length = 60)
    private String activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    /** Clé brute du message (TERRITORY_CHANNEL_ACTIVITY_...). */
    @Column(name = "message_key", length = 150)
    private String messageKey;

    /** Horodatage de l'événement en UTC (timestamp du fichier en millisecondes). */
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "author_id", length = 40)
    private String authorId;

    @Column(name = "author_name", length = 60)
    private String authorName;

    @Column(name = "state")
    private Integer state;

    @Column(name = "score_delta")
    private Long scoreDelta;

    /** Score cumulé de la zone après l'événement (0 pour les événements qui ne portent pas de score). */
    @Column(name = "score_total")
    private Long scoreTotal;

    @Column(name = "participation_delta")
    private Integer participationDelta;

    @Column(name = "participation_total")
    private Integer participationTotal;

    /** Zone source de l'événement (…_strike02, …_covert01) et son numéro ; vide pour deploy / étoiles. */
    @Column(name = "source_zone_id", length = 120)
    private String sourceZoneId;

    @Column(name = "source_numero")
    private Integer sourceNumero;

    /** STARS : nombre d'étoiles atteint dans la zone. */
    @Column(name = "etoiles")
    private Integer etoiles;

    /** STRIKE_* / COVERT : clé de libellé de mission fournie par le jeu. */
    @Column(name = "mission_name_key", length = 100)
    private String missionNameKey;

    /** STRIKE_* : progression « x / y » de la contribution. */
    @Column(name = "mission_faites")
    private Integer missionFaites;

    @Column(name = "mission_total")
    private Integer missionTotal;
}