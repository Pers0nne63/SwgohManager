package swgohManager.controller.dto;

import java.time.LocalDateTime;

public interface TbMSStatsProjection {
    LocalDateTime getEndTime();
    String getPlayerName(); // Corrigé en String
    
    Integer getQiraT();
    Integer getQiraW();
    Integer getJkckT();
    Integer getJkckW();
    Integer getSawT();
    Integer getSawW();
    Integer getRevaT();
    Integer getRevaW();
    Integer getBkmT();
    Integer getBkmW();
    Integer getMerrinT();
    Integer getMerrinW();
    Integer getClonesT();
    Integer getClonesW();
    Integer getInquisT();
    Integer getInquisW();
    Integer getL337T();
    Integer getL337W();
    Integer getYhanT();
    Integer getYhanW();
}