package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UnitRaw(
        String id,
        String baseId,
        Integer forceAlignment,
        Integer unitClass,
        Integer combatType,
        Boolean legend,
        String statProgressionId,
        List<String> categoryId,
        Integer primaryUnitStat,
        List<UnitTierRaw> unitTier,
        StatWrapper baseStat,
        RelicDefinitionRaw relicDefinition
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnitTierRaw(Integer tier, StatWrapper baseStat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatWrapper(List<StatEntry> stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(Integer unitStatId, String unscaledDecimalValue) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelicDefinitionRaw(List<String> relicTierDefinitionId) {}
}