package swgohManager.controller.dto;

import java.time.LocalDate;

public interface RosterProgressionJourProjection {
    LocalDate getJour();
    String getPlayerId();
    String getPlayerName();
    String getBaseId();
    String getLibelle();
    Boolean getNouvelleUnite();
    Boolean getOmicronObtenu();
    Integer getEtoilesAvant();
    Integer getEtoilesApres();
    Integer getGearAvant();
    Integer getGearApres();
    Integer getRelicAvant();
    Integer getRelicApres();
}