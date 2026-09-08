package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_import_score_joueur",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tb_import_activite_id", "playerId"}),
       indexes = {
               @Index(name = "idx_tb_imp_sj_playerId", columnList = "playerId")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbImportScoreJoueur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tb_import_activite_id", nullable = false)
    private TbImportActivite tbImportActivite;

    @Column(nullable = false)
    private String playerId;

    private Long score;
}