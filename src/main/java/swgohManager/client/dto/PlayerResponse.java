package swgohManager.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerResponse(
        String playerId,
        String allyCode,
        String name,
        String guildId,
        @JsonProperty("guildName") String guildName,
        List<ProfileStatEntry> profileStat,
        List<Stat> stat,
        PlayerRating playerRating,
        List<RosterUnit> rosterUnit,
        List<DatacronRaw> datacron,
        List<EraUnitStatusRaw> eraUnitStatus
) {

    // --- MÉTHODES UTILITAIRES POUR RÉCUPÉRER TES 3 STATS ---

    public Long getGalacticPower() {
        return extractStatValue("STAT_GALACTIC_POWER_ACQUIRED_NAME");
    }

    public Long getCharacterGalacticPower() {
        return extractStatValue("STAT_CHARACTER_GALACTIC_POWER_ACQUIRED_NAME");
    }

    public Long getShipGalacticPower() {
        return extractStatValue("STAT_SHIP_GALACTIC_POWER_ACQUIRED_NAME");
    }

    private Long extractStatValue(String targetKey) {
        if (profileStat == null) {
            return 0L;
        }
        return profileStat.stream()
                .filter(s -> targetKey.equals(s.nameKey()))
                .map(s -> Long.parseLong(s.value())) // Convertit le String en Long
                .findFirst()
                .orElse(0L); // Retourne 0 si la stat n'est pas trouvée
    }

    // --- SOUS-RECORDS ---

    // Nouveau record pour mapper les éléments du tableau JSON
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileStatEntry(
            String nameKey,
            String value
    ) {}
    
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