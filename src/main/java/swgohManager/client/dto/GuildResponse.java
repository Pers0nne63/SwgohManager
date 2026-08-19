package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuildResponse(Guild guild) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guild(
            String id,
            String name,
            List<Member> member,
            List<RecentRaidResult> recentRaidResult,
            List<TerritoryBattleResult> recentTerritoryBattleResult
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