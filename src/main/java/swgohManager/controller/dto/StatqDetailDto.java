package swgohManager.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatqDetailDto {
    private String team;
    private String baseId;
    private String nomUnite;
    private Integer statId;
    private String nomStat;
    private Double valeurActuelle;
    private Double valeurObjectif;
    private Double variation;
    private Integer note;
}