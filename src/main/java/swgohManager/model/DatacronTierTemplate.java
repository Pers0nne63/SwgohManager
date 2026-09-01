package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "datacron_tier_template", indexes = {
        @Index(name = "idx_dc_tier_template_id", columnList = "idTemplate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatacronTierTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idTemplate;

    private Integer tierId;
    private Integer requiredUnitTier;
    private Integer requiredRelicTier;
    private String overrideUpgradeCostRecipeId;
    private Integer overrideScopeIdentifier;
}