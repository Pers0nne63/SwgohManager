package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerResponse(
        String playerId,
        String allyCode,
        String name,
        PlayerRating playerRating,
        List<RosterUnit> rosterUnit
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerRating(PlayerSkillRating playerSkillRating, PlayerRankStatus playerRankStatus) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerSkillRating(Integer skillRating) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerRankStatus(String leagueId, Integer divisionId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RosterUnit(
            String id,
            String definitionId,
            Integer currentRarity,
            Integer currentLevel,
            Integer currentTier,
            Relic relic,
            List<Skill> skill,
            List<EquippedStatMod> equippedStatMod
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Relic(Integer currentTier) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Skill(String id, Integer tier) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EquippedStatMod(
            String id,
            String definitionId,
            Integer level,
            PrimaryStat primaryStat,
            List<SecondaryStat> secondaryStat
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrimaryStat(Stat stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SecondaryStat(Stat stat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stat(Integer unitStatId, String unscaledDecimalValue) {}
}