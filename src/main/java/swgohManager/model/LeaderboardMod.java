package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "leaderboard_mods", uniqueConstraints = @UniqueConstraint(columnNames = {"idMod", "ordreSecondaire"}),
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "Lmodidx_playerId", columnList = "playerId"),
        @Index(name = "Lmodidx_definitionId", columnList = "definitionId"),
        @Index(name = "Lmodidx_idSecondaire", columnList = "idSecondaire"),
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardMod implements ModLigne {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String idUnit;
    private String idMod;
    private String definitionId;

    private String set;
    private String rarity;
    private String position;
    private Integer niveau;

    private Integer idPrimaire;
    private String primaire;
    private Long valeurPrimaire;

    private Integer idSecondaire;
    private String secondaire;
    private Long valeurSecondaire;
    private Integer ordreSecondaire;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}