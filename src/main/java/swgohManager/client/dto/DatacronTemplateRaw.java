package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DatacronTemplateRaw(
        String id,
        Integer setId,
        Integer initialTiers,
        String referenceTemplateId,
        Integer maxRerolls,
        Boolean allowReroll,
        Boolean focused,
        String focusedIcon,
        String focusedPrefab,
        List<String> fixedTag,
        List<DatacronTierRaw> tier
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DatacronTierRaw(
            Integer id,
            Integer requiredUnitTier,
            Integer requiredRelicTier,
            String overrideUpgradeCostRecipeId,
            Integer overrideScopeIdentifier,
            List<String> affixTemplateSetId,
            List<String> initialAffixTemplateSetIds
    ) {}
}