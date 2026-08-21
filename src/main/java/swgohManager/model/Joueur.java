package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "joueurs",
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "idx_playerId", columnList = "playerId")
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Joueur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String playerId;
    private String GuildId;
    private String playerName;
    private Long galacticPower;
    private String leagueId;
    private Long shipGalacticPower;
    private Long characterGalacticPower;

    @Builder.Default
    private boolean presentInGuild = true;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}