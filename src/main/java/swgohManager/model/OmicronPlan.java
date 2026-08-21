package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "omicron_plan",
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "omiidx_baseId", columnList = "baseId"),
        @Index(name = "omiidx_idSkill", columnList = "idSkill")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OmicronPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseId;

    @Column(nullable = false)
    private String idSkill;

    private Integer priorite;
}