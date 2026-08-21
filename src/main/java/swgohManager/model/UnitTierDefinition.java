package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unit_tier_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "gear", "stat"}),
       indexes = {
   	        @Index(name = "utdefidx_idUnit", columnList = "idUnit")
   			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitTierDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private Integer gear;
    private Integer stat;
    private Long valeur;
}