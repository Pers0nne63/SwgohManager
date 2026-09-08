package swgohManager.controller.dto;

import java.util.List;

import lombok.Data;

@Data
public class TbImportStatWrapper {
    private List<TbImportMapStat> currentStat;
}