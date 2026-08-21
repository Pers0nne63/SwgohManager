package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_activite",
       uniqueConstraints = @UniqueConstraint(columnNames = {"territory_battle_id", "mapStatId"}),
       indexes = {
   	        @Index(name = "idx_territory_battle_id", columnList = "territory_battle_id"),
   	        @Index(name = "idx_statType", columnList = "statType")
   			})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbActivite {

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