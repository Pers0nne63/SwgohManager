package swgohManager.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmPlanDto {
    private Long id;
    private String baseId;
    private String nomUnite;
    private Integer etoilesCible;
    private Integer relicCible;
    private String tag;
}