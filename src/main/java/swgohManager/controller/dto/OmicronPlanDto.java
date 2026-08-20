package swgohManager.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmicronPlanDto {
    private Long id;
    private String baseId;
    private String nomUnite;
    private String idSkill;
    private String nomSkill;
    private Integer priorite;
}