package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventsResponse(List<GameEvent> gameEvent) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameEvent(String id, String nameKey, List<Instance> instance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Instance(String id) {}
}