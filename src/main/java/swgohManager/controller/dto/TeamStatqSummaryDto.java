package swgohManager.controller.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamStatqSummaryDto {
    private String team;
    private int scoreTotal;
    private double noteMoyenne;
    private List<StatqDetailDto> details;
}