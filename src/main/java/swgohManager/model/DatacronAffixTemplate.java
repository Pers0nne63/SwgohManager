package swgohManager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}