package swgohManager.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BattleTargetingRuleRaw(
        String id,
        CategoryFilterRaw category
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryFilterRaw(
            List<CategoryEntryRaw> category
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryEntryRaw(
            String categoryId,
            Boolean exclude
    ) {}
}