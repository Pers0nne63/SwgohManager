package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuildResponse(Guild guild) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guild(
            String id,
            String name,
            GuildProfile profile,
            List<Member> member,
            List<RecentRaidResult> recentRaidResult,
            List<TerritoryBattleResult> recentTerritoryBattleResult
    ) {
        /**
         * Récupère le nom de la guilde qu'il soit directement sous 'guild'
         * ou imbriqué dans 'profile'.
         */
        public String getGuildName() {
            if (name != null && !name.isBlank()) {
                return name;
            }
            return profile != null ? profile.name() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GuildProfile(
            String id,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Member(
            String playerId, String playerName, String galacticPower,
            String leagueId, String shipGalacticPower, String characterGalacticPower
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecentRaidResult(long endTime, List<RaidMember> raidMember) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RaidMember(String playerId, long memberProgress) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TerritoryBattleResult(
            String instanceId,
            String definitionId,
            String startTime,
            String endTime,
            String totalStars,
            List<FinalStat> finalStat
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinalStat(String mapStatId, List<PlayerStat> playerStat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStat(String memberId, String score) {}
}