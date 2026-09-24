package swgohManager.model;

import java.time.Instant;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}