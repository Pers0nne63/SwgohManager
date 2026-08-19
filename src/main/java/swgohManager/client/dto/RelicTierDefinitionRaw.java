package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelicTierDefinitionRaw(
        String id,
        StatWrapper stat,
        String relicStatTable,
        Integer tier
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatWrapper(List<StatEntry> stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(Integer unitStatId, String unscaledDecimalValue) {}
}