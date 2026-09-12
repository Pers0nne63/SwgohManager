package swgohManager.model;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swgohManager.config.BatchConstants;
@Entity
@Table(name = "external_player_statq_detail_actuel", indexes = { @Index(name = "epsdaidx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerStatqDetailActuel implements StatqDetailValues {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_player_statq_detail_actuel_seq")
    @SequenceGenerator(name = "external_player_statq_detail_actuel_seq", sequenceName = "external_player_statq_detail_actuel_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
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