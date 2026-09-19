package swgohManager.controller.dto;

import java.time.LocalDateTime;

public interface TbSyntheseRoundProjection {
    Long getTerritoryBattleId();
    LocalDateTime getEndTime();
    Integer getRoundNum();
    String getPlayerId();
    Long getCombats();
    Long getVagues();
}