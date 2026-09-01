package swgohManager.repository;

public interface EraUnitPlayerProjection {
	String getPlayerId();
    String getPlayerName();
    String getUnitBaseId();
    String getLibelle();
    Integer getEraLevel();
    Integer getRarity(); // Le nombre d'étoiles (current_rarity)
}