package swgohManager.controller.dto;

import java.util.List;

import lombok.Data;

@Data
public class TbImportMapStat {
    private String mapStatId;
    private List<TbImportPlayerStat> playerStat;
}