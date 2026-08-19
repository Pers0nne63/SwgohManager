package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "stat_progression",
       uniqueConstraints = @UniqueConstraint(columnNames = {"statProgressionId", "unitStatId"}))
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