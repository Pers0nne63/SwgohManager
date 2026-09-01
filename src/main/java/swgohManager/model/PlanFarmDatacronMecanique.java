package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_farm_datacron_mecanique")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanFarmDatacronMecanique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planFarmDatacronId;

    private Integer tier;
    private String abilityId;
}