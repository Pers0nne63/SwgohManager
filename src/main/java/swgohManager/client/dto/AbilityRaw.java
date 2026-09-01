package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbilityRaw(
        String id,
        String nameKey,
        String descKey,
        String shortDescKey,
        String icon,
        Integer cooldown,
        Integer abilityType,
        Integer buttonLocation,
        Integer detailLocation,
        Integer cooldownType,
        Boolean useAsReinforcementDesc,
        Boolean alwaysDisplayInBattleUi,
        Boolean highlightWhenReadyInBattleUi,
        Boolean hideCooldownDescription,
        String blockingEffectId,
        String blockedLocKey,
        Integer grantedPriority,
        String prefabName,
        String subIcon,
        String allyTargetingRuleId
) {}