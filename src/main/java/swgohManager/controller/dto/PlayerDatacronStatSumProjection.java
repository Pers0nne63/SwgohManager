package swgohManager.controller.dto;

import java.math.BigDecimal;

public interface PlayerDatacronStatSumProjection {
    String getPlayerId();
    String getIdDatacron();
    String getSetId();
    String getStatType();
    BigDecimal getValue();
}