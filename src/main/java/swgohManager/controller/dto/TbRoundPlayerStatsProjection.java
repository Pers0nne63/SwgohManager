package swgohManager.controller.dto;

public interface TbRoundPlayerStatsProjection {
	Integer getRoundNum();
	String getPlayerId();
    Long getPower();
    Long getSummary();
    Long getStrikeAttempt();
    Long getStrikeEncounter();
    Long getCovertAttempt();
}