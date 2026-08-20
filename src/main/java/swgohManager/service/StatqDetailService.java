package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.controller.dto.StatqDetailDto;
import swgohManager.controller.dto.TeamStatqSummaryDto;
import swgohManager.model.PlayerStatqDetailActuel;
import swgohManager.model.StatDefinition;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.PlayerStatqDetailActuelRepository;
import swgohManager.repository.StatDefinitionRepository;
import swgohManager.repository.UnitDefinitionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatqDetailService {

    private final PlayerStatqDetailActuelRepository playerStatqDetailActuelRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public List<TeamStatqSummaryDto> getDetailParTeam(String playerId) {
        List<PlayerStatqDetailActuel> details = playerStatqDetailActuelRepository.findByPlayerId(playerId);

        Map<Integer, String> statMap = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        StatDefinition::getStatId,
                        d -> d.getLibellé() != null ? d.getLibellé() : String.valueOf(d.getStatId()),
                        (v1, v2) -> v1
                ));

        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(
                        UnitDefinition::getBaseId,
                        UnitDefinition::getLibelle,
                        (v1, v2) -> v1
                ));

        // Groupement temporaire par nom de team
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
                .collect(Collectors.groupingBy(
                        StatqDetailDto::getTeam,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Construction du résumé par team + calculs + tri par moyenne croissante
        return grouped.entrySet().stream()
                .map(entry -> {
                    String teamName = entry.getKey();
                    List<StatqDetailDto> teamDetails = entry.getValue();

                    int scoreTotal = teamDetails.stream()
                            .map(StatqDetailDto::getNote)
                            .filter(Objects::nonNull)
                            .mapToInt(Integer::intValue)
                            .sum();

                    double noteMoyenne = teamDetails.stream()
                            .map(StatqDetailDto::getNote)
                            .filter(Objects::nonNull)
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);

                    return TeamStatqSummaryDto.builder()
                            .team(teamName)
                            .scoreTotal(scoreTotal)
                            .noteMoyenne(noteMoyenne)
                            .details(teamDetails)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TeamStatqSummaryDto::getNoteMoyenne))
                .toList();
    }
}