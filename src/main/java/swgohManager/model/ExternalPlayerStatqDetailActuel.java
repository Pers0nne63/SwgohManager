package swgohManager.model;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "external_player_statq_detail_actuel", indexes = { @Index(name = "epsdaidx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerStatqDetailActuel implements StatqDetailValues {
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
}