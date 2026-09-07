package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "external_player", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"),
        indexes = { @Index(name = "extplayeridx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String playerId;
    private String guildId;
    private String guildName;
    private String allyCode;
    private String playerName;
    private Long galacticPower;
    private Long characterGalacticPower;
    private Long shipGalacticPower;
    private String leagueId;
    private Integer skillRating;
    private Integer divisionId;
    @Column(nullable = false)
    private Instant dateScan; // rempli manuellement à chaque scan, sert de base pour la purge à 30j
}