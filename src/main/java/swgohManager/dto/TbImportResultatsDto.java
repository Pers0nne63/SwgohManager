package swgohManager.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TbImportResultatsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundImportDTO {
        private Integer roundNum;
        private Map<String, String> planetNames;
        private List<JoueurImportDTO> joueurs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoueurImportDTO {
        private String playerName;
        private Map<String, PlanetStatDTO> planetStats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanetStatDTO {
        private Integer combats;
        private Integer vagues;
        private Long pgDeploye;    // Correspond à power
        private Long pointsCombat; // Résultat calculé
        private Long rawSummary;   // Stocke le summary brut pour le calcul
    }
}