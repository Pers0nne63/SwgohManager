package swgohManager.controller.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class PlayerEraRowDTO {
	private String playerId;
    private String playerName;
    // Clé: unitBaseId, Valeur: Le texte formaté (ex: "7★ - N3")
    private Map<String, String> unitData = new HashMap<>();
}