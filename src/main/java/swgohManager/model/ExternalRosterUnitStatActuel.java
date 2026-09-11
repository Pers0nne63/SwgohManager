package swgohManager.model;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "external_roster_unit_stat_actuel", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "idUnit"}),
indexes = { @Index(name = "erusaidx_playerId", columnList = "playerId"), @Index(name = "erusaidx_idUnit", columnList = "idUnit") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalRosterUnitStatActuel implements UnitStatValues {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_roster_unit_stat_actuel_seq")
    @SequenceGenerator(name = "external_roster_unit_stat_actuel_seq", sequenceName = "external_roster_unit_stat_actuel_seq", allocationSize = 50)
    private Long id;
    private String playerId;
    private String idUnit;
    private Double sante;
    private Double protection;
    private Double vitesse;
    private Double attaquePhysique;
    private Double attaqueSpeciale;
    private Double armure;
    private Double resistance;
    private Double penetrationArmure;
    private Double penetrationResistance;
    private Double esquive;
    private Double deviation;
    private Double ccPhysique;
    private Double ccSpeciaux;
    private Double degatsCritiques;
    private Double pouvoir;
    private Double tenacite;
    private Double volDeSante;
    private Double precisionPhysique;
    private Double precisionSpeciale;
    private Double esquiveCritiquePhysique;
    private Double esquiveCritiqueSpeciale;
    private Double defense;
}