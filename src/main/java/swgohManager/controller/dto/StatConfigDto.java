package swgohManager.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatConfigDto {
    private Long id;
    private String team;
    private String baseId;
    private String nomUnite;
    private Integer statId1;
    private Integer statId2;
    private Integer statId3;
    private Integer statId4;
}