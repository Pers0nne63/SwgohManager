package swgohManager.controller.dto;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OmicronUnitDTO {
    private String baseId;
    private String unitName;
    private List<SkillDTO> skills;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillDTO {
        private String idSkill;
        private String skillType;
        private Integer numero;
    }
}