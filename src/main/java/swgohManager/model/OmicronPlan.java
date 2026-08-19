package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "omicron_plan")
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