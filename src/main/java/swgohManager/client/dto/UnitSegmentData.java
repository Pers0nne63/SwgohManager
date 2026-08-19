package swgohManager.client.dto;

import java.util.List;

public record UnitSegmentData(List<UnitRaw> units, List<RelicTierDefinitionRaw> relicTierDefinitions) {}