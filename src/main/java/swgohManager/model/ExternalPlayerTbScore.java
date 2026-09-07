package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "external_player_tb_score",
        indexes = { @Index(name = "extptbidx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerTbScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String playerId;
    private String guildId;
    private String tbInstanceId;
    private String tbDefinitionId;
    private Instant tbStartTime;
    private Instant tbEndTime;
    private Integer totalStars;
    private String mapStatId;
    private String statType;
    private Integer phase;
    private Integer conflict;
    private Boolean bonus;
    private Integer covertNum;
    private Integer roundNum;
    private Long score;
}