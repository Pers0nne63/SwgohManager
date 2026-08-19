package swgohManager.controller.dto;

import java.time.LocalDateTime;

public interface TbRoundStatsProjection {
    Long getTerritoryBattleId();
    LocalDateTime getEndTime();
    Integer getTotalStars();
    Integer getRoundNum();
    Long getVagues();
    Long getMsTentees();
}