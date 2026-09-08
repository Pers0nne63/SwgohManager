package swgohManager.model;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "external_player_statq_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerStatqActuel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerId;
    private Double statq;
    private Integer nbStats;
}