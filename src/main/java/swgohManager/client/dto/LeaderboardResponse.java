package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaderboardResponse(List<Leaderboard> leaderboard) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leaderboard(List<PlayerEntry> player) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerEntry(String id, String power) {}
}