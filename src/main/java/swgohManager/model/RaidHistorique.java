package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "raid_historique",
       uniqueConstraints = @UniqueConstraint(columnNames = {"guildId", "playerId", "endTime"}),
		indexes = {
		        @Index(name = "raidhidx_playerId", columnList = "playerId")
				}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RaidHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guildId;
    private String playerId;
    private Long score;
    private Instant endTime;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant dateInsertion;
}