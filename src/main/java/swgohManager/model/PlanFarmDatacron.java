package swgohManager.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_farm_datacron")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanFarmDatacron {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String setId;

    private String nom;

    private LocalDateTime dateCreation;
}