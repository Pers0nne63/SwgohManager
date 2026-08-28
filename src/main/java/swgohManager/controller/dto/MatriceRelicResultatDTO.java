package swgohManager.controller.dto;

import java.util.Map;

public record MatriceRelicResultatDTO(
    String playerName,
    double pourcentageActif,
    Map<String, Integer> relicsPossedes,     // baseId -> niveau de relic du joueur (-1 si < G13)
    Map<String, Boolean> objectifsAtteints   // baseId -> true si relic >= cible
) {}