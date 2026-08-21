package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farm_plan",
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "pdfidx_baseId", columnList = "baseId")
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FarmPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseId;

    private Integer etoilesCible;
    private Integer relicCible;
    private String tag;
}