package swgohManager.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.StatqDetailDto;
import swgohManager.controller.dto.TeamStatqSummaryDto;
import swgohManager.model.StatDefinition;
import swgohManager.model.StatqDetailValues;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.ExternalPlayerStatqDetailActuelRepository;
import swgohManager.repository.PlayerStatqDetailActuelRepository;
import swgohManager.repository.StatDefinitionRepository;
import swgohManager.repository.UnitDefinitionRepository;

@Service
@RequiredArgsConstructor
public class StatqDetailService {

    private final PlayerStatqDetailActuelRepository playerStatqDetailActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalPlayerStatqDetailActuelRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public List<TeamStatqSummaryDto> getDetailParTeam(String playerId, Portee portee) {
        List<? extends StatqDetailValues> details = (portee == Portee.GUILDE)
                ? playerStatqDetailActuelRepository.findByPlayerId(playerId)
                : externalPlayerStatqDetailActuelRepository.findByPlayerId(playerId);
        return construire(details);
    }

    private List<TeamStatqSummaryDto> construire(List<? extends StatqDetailValues> details) {
        Map<Integer, String> statMap = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        StatDefinition::getStatId,
                        d -> d.getLibellé() != null ? d.getLibellé() : String.valueOf(d.getStatId()),
                        (v1, v2) -> v1
                ));

        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));

        Map<String, List<StatqDetailDto>> grouped = details.stream()
                .map(d -> StatqDetailDto.builder()
                        .team(d.getTeam() != null ? d.getTeam() : "Sans équipe")
                        .baseId(d.getBaseId())
                        .nomUnite(unitMap.getOrDefault(d.getBaseId(), d.getBaseId()))
                        .statId(d.getStatId())
                        .nomStat(statMap.getOrDefault(d.getStatId(), String.valueOf(d.getStatId())))
                        .valeurActuelle(d.getValeurActuelle())
                        .valeurObjectif(d.getValeurObjectif())
                        .variation(d.getVariation())
                        .note(d.getNote())
                        .build())
                .collect(Collectors.groupingBy(StatqDetailDto::getTeam, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String teamName = entry.getKey();
                    List<StatqDetailDto> teamDetails = entry.getValue();

                    int scoreTotal = teamDetails.stream()
                            .map(StatqDetailDto::getNote).filter(Objects::nonNull)
                            .mapToInt(Integer::intValue).sum();

                    double noteMoyenne = teamDetails.stream()
                            .map(StatqDetailDto::getNote).filter(Objects::nonNull)
                            .mapToInt(Integer::intValue).average().orElse(0.0);

                    return TeamStatqSummaryDto.builder()
                            .team(teamName).scoreTotal(scoreTotal).noteMoyenne(noteMoyenne).details(teamDetails)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TeamStatqSummaryDto::getNoteMoyenne))
                .toList();
    }
}