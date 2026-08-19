package swgohManager.client.dto;

public record GetLeaderboardRequest(Payload payload) {
    public record Payload(Integer leaderboardType, String eventInstanceId, String groupId) {}
}