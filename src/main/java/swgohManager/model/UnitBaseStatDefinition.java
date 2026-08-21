package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unit_base_stat_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "stat"}),
       indexes = {
     	        @Index(name = "ubsdefidx_idUnit", columnList = "idUnit")
     			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitBaseStatDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private Integer stat;
    private Long valeur;
}