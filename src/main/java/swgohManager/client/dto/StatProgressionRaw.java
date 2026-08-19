package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatProgressionRaw(String id, StatWrapper stat) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatWrapper(List<StatEntry> stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(Integer unitStatId, String unscaledDecimalValue) {}
}