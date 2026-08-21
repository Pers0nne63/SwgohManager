package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_statq_detail_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "baseId", "statId"}),
		indexes = {
		        @Index(name = "statqdetidx_playerId", columnList = "playerId"),
		        @Index(name = "statqdetidx_baseId", columnList = "baseId")
				}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerStatqDetailActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String baseId;
    private String team;
    private Integer statId;

    private Double valeurActuelle;
    private Double valeurObjectif;
    private Double variation;
    private Integer note;

    private Long idSync;
}