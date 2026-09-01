package swgohManager.controller.dto;

import java.math.BigDecimal;

public interface PlayerDatacronProjection {
    String getPlayerId();
    String getIdDatacron();
    Boolean getFocused();
    String getFocusLibelle();
    String getSetId();
    Integer getOrdre();
    String getTarget();
    String getDescription();
    BigDecimal getValue();
}