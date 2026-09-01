package swgohManager.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_farm_datacron_stat")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanFarmDatacronStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planFarmDatacronId;

    private String statType;
    private BigDecimal statValue;
}