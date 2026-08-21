package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_statq_historique",
		indexes = {
        @Index(name = "statqhidx_playerId", columnList = "playerId")
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerStatqHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private Double statq;
    private Integer nbStats;
    private Long idSync;
}