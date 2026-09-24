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
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}