package swgohManager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ability_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbilityDefinition {

    @Id
    private String id;

    private String nameKey;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String descKey;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String shortDescKey;

    private String icon;
    private Integer cooldown;
    private Integer abilityType;
    private Integer buttonLocation;
    private Integer detailLocation;
    private Integer cooldownType;
    private Boolean useAsReinforcementDesc;
    private String blockingEffectId;
    private String blockedLocKey;
    private Integer grantedPriority;
    private String subIcon;
    private String allyTargetingRuleId;
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}