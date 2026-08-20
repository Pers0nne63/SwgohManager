package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_statq_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerStatqActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerId;

    private Double statq;
    private Integer nbStats;
    private Long idSync;
}