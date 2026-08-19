package swgohManager.client.dto;

public record GuildRequest(Payload payload) {

    public record Payload(String guildId, boolean includeRecentGuildActivityInfo) {}

    public static GuildRequest of(String guildId) {
        return new GuildRequest(new Payload(guildId, true));
    }
}