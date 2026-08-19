package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farm_plan")
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