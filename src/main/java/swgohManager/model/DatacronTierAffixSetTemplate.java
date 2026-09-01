package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "datacron_tier_affix_set_template", indexes = {
        @Index(name = "idx_dc_tier_affix_set", columnList = "idTemplate, tierId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatacronTierAffixSetTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idTemplate;

    private Integer tierId;
    private String affixTemplateSetId;
}