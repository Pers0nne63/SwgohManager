package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unit_relic_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "relicTierDefinitionId"}),
       indexes = {
   	        @Index(name = "urdefidx_idUnit", columnList = "idUnit")
   			}
   )
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitRelicDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private String relicTierDefinitionId;
}