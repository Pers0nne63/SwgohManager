package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mastery_stat", uniqueConstraints = @UniqueConstraint(columnNames = {"masteryClass", "unitStatId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MasteryStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String masteryClass;

    @Column(nullable = false)
    private Integer unitStatId;

    private Double value;
}