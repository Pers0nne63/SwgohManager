package swgohManager.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Boolean conquete;
    private String statProgressionId;
    private String role;
    private String primaryStat;
    private String masteryClass;

    private String gameVersion;

    
    @UpdateTimestamp
    private Instant dateMiseAJour;
}