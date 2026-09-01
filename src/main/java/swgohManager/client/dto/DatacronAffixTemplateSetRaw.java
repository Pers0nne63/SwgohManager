package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DatacronAffixTemplateSetRaw(
        String id,
        List<AffixRaw> affix
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AffixRaw(
            String abilityId,
            Integer statType,
            String statValueMin,
            String statValueMax,
            String scopeIcon,
            String targetRule
    ) {}
}