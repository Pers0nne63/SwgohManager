package swgohManager.dto.pivot;

import lombok.Builder;

import java.util.List;

@Builder
public record DatacronDTO(
        String idDatacron,
        Integer setId,
        String templateId,
        Boolean locked,
        Integer rerollIndex,
        Integer rerollCount,
        Boolean focused,
        List<AffixDTO> affixes
) {
    @Builder
    public record AffixDTO(
            Integer ordre,
            String tag,
            String targetRule,
            String abilityId,
            Integer statType,
            Long statValue,
            Integer requiredUnitTier,
            Integer requiredRelicTier,
            String scopeIcon
    ) {}
}