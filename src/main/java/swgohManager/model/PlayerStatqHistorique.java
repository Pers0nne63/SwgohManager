package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_statq_historique")
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