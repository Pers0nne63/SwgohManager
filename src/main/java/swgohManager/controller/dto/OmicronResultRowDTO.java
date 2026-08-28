package swgohManager.controller.dto;
import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OmicronResultRowDTO {
    private String playerId;
    private String playerName;
    // Clé: idSkill, Valeur: true si possédé, false sinon
    private Map<String, Boolean> omicronsPossedes;
    private int totalPossedes;
    private double pourcentageActif;
}