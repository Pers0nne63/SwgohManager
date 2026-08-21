package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_score_joueur",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tb_activite_id", "playerId"}),
       indexes = {
      	        @Index(name = "tbsjidx_playerId", columnList = "playerId")
      			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbScoreJoueur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tb_activite_id", nullable = false)
    private TbActivite tbActivite;

    @Column(nullable = false)
    private String playerId;

    private Long score;
}