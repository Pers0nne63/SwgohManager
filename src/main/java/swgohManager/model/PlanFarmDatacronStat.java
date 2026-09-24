package swgohManager.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}