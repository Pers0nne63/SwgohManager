package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_plan_round", uniqueConstraints = @UniqueConstraint(columnNames = {"planId", "roundNum"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbPlanRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Integer roundNum;

    private Long lsPlaneteId;
    private Long dsPlaneteId;
    private Long mixPlaneteId;
    private Long zeffoPlaneteId;
    private Long mandalorePlaneteId;
}