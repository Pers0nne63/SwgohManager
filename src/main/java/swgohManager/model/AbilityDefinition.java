package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

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
}