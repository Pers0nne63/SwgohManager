package swgohManager.model;

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

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}