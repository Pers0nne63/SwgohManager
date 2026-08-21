package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "relic_tier_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idRelicTier", "stat"}),
		indexes = {
		        @Index(name = "relicdefidx_idRelicTier", columnList = "idRelicTier")
				})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RelicTierDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idRelicTier;

    private String relicStatTable;
    private Integer tierRelic;
    private Integer relic; // tierRelic - 2, null si négatif

    private Integer stat;
    private Long valeur;
}