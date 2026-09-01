package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "datacron_affix_template", indexes = {
        @Index(name = "idx_dc_affix_template_id", columnList = "idTemplate"),
        @Index(name = "idx_dc_affix_set_id", columnList = "affixSetId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatacronAffixTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idTemplate;        

    private String affixSetId;       
    private Integer tier;         
    private String statType;       
    private String statValueMin;   
    private String statValueMax;    
    private String scopeIcon;       
    private String targetRule;
    private String abilityId;
}