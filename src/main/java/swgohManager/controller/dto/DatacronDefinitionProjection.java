package swgohManager.controller.dto;

import java.math.BigDecimal;

public interface DatacronDefinitionProjection {
    String getIdTemplate();
    String getSet();
    Boolean getFocused();
    String getFocusLibelle();
    Integer getTier();
    String getAbilityId();
    String getLibelle();
    String getDescription();
    String getStatType();
    String getStatLibelle();
    BigDecimal getStatMin();
    BigDecimal getStatMax();
    String getTarget();
}