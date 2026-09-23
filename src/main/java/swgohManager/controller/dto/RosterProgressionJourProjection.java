package swgohManager.controller.dto;

public interface RosterProgressionJourProjection {
    java.time.LocalDate getJour();
    String getPlayerId();
    String getPlayerName();
    String getBaseId();
    String getLibelle();
    Boolean getNouvelleUnite();
    Integer getEtoilesAvant();
    Integer getEtoilesApres();
    Integer getGearAvant();
    Integer getGearApres();
    Integer getRelicAvant();
    Integer getRelicApres();
}