package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_modq_historique",
		indexes = {
        @Index(name = "modqhidx_playerId", columnList = "playerId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerModQHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;

    private Integer mod25Plus;
    private Integer mod20_24;
    private Integer mod15_19;
    private Integer mod10_14;
    private Double modQ;

    private Long idSync;
}