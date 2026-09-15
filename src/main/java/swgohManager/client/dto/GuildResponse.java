package swgohManager.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import swgohManager.client.dto.GuildResponse.GuildProfile;
import swgohManager.client.dto.GuildResponse.RecentRaidResult;
import swgohManager.client.dto.GuildResponse.TerritoryBattleResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuildResponse(Guild guild) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guild(
            String id,
            String name,
            GuildProfile profile,
            List<Member> member,
            List<RecentRaidResult> recentRaidResult,
            List<TerritoryBattleResult> recentTerritoryBattleResult,
            List<TerritoryWarResult> recentTerritoryWarResult

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
            String name,
            Integer guildGalacticPower
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
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TerritoryWarResult(
            String territoryWarId,
            String score,
            Integer power,              
            String opponentScore,
            String startTime,           
            String endTimeSeconds,      
            OpponentGuildProfile opponentGuildProfile
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpponentGuildProfile(
            String id,
            String name,
            String guildGalacticPower
    ) {}
}