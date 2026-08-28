package swgohManager.controller.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitHeaderDTO {
    private String unitName;
    private List<SkillHeaderDTO> skills;
}