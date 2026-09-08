package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_import_activite",
       uniqueConstraints = @UniqueConstraint(columnNames = {"territory_battle_id", "mapStatId", "roundNum"}),
       indexes = {
               @Index(name = "idx_tb_imp_act_tb_id", columnList = "territory_battle_id"),
               @Index(name = "idx_tb_imp_act_statType", columnList = "statType")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbImportActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_battle_id", nullable = false)
    private TerritoryBattle territoryBattle;

    @Column(nullable = false)
    private String mapStatId;

    private String statType;
    private Integer phase;
    private Integer conflict;
    private boolean bonus;
    private Integer covertNum;
    private Integer roundNum;
}