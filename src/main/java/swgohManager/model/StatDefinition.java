package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stat_definition", uniqueConstraints = @UniqueConstraint(columnNames = "statId"),
indexes = {
        @Index(name = "stdefidx_statId", columnList = "statId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer statId;

    private String nameKey;
    private String descKey;
    private Boolean isDecimal;
    private String name;
    private String detailedName;
    private String libellé;
    private Boolean isStatq;
}