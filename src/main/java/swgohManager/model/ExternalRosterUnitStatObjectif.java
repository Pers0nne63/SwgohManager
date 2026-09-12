package swgohManager.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swgohManager.config.BatchConstants;
@Entity
@Table(name = "external_roster_unit_stat_objectif", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "idUnit"}),
indexes = { @Index(name = "erusoidx_playerId", columnList = "playerId"), @Index(name = "erusoidx_idUnit", columnList = "idUnit") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalRosterUnitStatObjectif implements UnitStatValues {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_roster_unit_stat_objectif_seq")
    @SequenceGenerator(name = "external_roster_unit_stat_objectif_seq", sequenceName = "external_roster_unit_stat_objectif_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
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