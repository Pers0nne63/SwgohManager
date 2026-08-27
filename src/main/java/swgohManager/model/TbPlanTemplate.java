package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_plan_template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbPlanTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private Integer etoilesCibles;
}