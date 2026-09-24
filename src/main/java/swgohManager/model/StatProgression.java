package swgohManager.model;

import java.time.Instant;

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
@Table(name = "stat_progression",
       uniqueConstraints = @UniqueConstraint(columnNames = {"statProgressionId", "unitStatId"}),
       indexes = {
    	        @Index(name = "stprogidx_statProgressionId", columnList = "statProgressionId")
    			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatProgression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String statProgressionId;

    @Column(nullable = false)
    private Integer unitStatId;

    private Long valeur;

    private String gameVersion;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}