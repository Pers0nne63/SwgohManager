package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "leaderboard_unit", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "idUnit"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String idUnit;
    private String definitionId;
    private Integer etoiles;
    private Integer niveau;
    private Integer gear;
    private Integer relic;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}