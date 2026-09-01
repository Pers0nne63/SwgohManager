package swgohManager.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerResponse(
        String playerId,
        String allyCode,
        String name,
        PlayerRating playerRating,
        List<RosterUnit> rosterUnit,
        List<DatacronRaw> datacron,
        List<EraUnitStatusRaw> eraUnitStatus
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
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DatacronRaw(
            String id, Integer setId, String templateId, Boolean locked,
            Integer rerollIndex, Integer rerollCount, Boolean focused,
            List<AffixRaw> affix
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AffixRaw(
            List<String> tag, String targetRule, String abilityId,
            Integer statType, String statValue,
            Integer requiredUnitTier, Integer requiredRelicTier, String scopeIcon
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EraUnitStatusRaw(String unitBaseId, Integer eraLevel) {}
}