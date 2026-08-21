package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "unit_definition", uniqueConstraints = @UniqueConstraint(columnNames = "idUnit"),
indexes = {
	        @Index(name = "udefidx_baseId", columnList = "baseId"),
	        @Index(name = "udefidx_idUnit", columnList = "idUnit")
			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private String baseId;
    private String libelle;
    private Integer forceAlignment; // 1=LS, 2=DS, 3=Neutre
    private Integer unitClass;
    private Integer combatType;
    private Boolean legend;
    private String statProgressionId;
    private String role;
    private String primaryStat;
    private String masteryClass;

    private String gameVersion;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}