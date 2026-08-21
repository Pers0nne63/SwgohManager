package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "leaderboard_unit", uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "idUnit"}),
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "LUnitidx_playerId", columnList = "playerId")
		}
)
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