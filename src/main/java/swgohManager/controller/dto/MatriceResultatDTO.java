package swgohManager.controller.dto;

import java.util.Map;

public record MatriceResultatDTO(
    String playerName,
    double pourcentageActif,
    Map<String, Boolean> omicronsPossedes
) {}