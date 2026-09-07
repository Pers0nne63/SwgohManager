package swgohManager.service;

import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.FarmPlan;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.FarmPlanRepository;
import swgohManager.repository.UnitDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalFarmPlanComparisonService {

    private final FarmPlanRepository farmPlanRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    // Réutilise la même forme que FarmPlanProgressService pour rester compatible avec un éventuel template partagé
    public record DetailRow(String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}
    public record ExternalFarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}

    public ExternalFarmProgress comparer(String playerId) {
        List<FarmPlan> plans = farmPlanRepository.findAll();
        if (plans.isEmpty()) {
            return new ExternalFarmProgress(0, 0, null, List.of());
        }

        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));

        Map<String, RosterBaseIdProgressProjection> parBaseId = externalRosterUnitActuelRepository
                .findMaxEtoilesRelicByBaseId(playerId).stream()
                .collect(Collectors.toMap(RosterBaseIdProgressProjection::getBaseId, p -> p));

        List<DetailRow> details = new ArrayList<>();
        int atteint = 0;

        for (FarmPlan plan : plans) {
            RosterBaseIdProgressProjection p = parBaseId.get(plan.getBaseId());
            int etoilesActuelles = (p != null && p.getMaxEtoiles() != null) ? p.getMaxEtoiles() : 0;
            Integer relicActuel = (p != null) ? p.getMaxRelic() : null;
            int relicPourComparaison = (relicActuel != null) ? relicActuel : 0;

            boolean ok = etoilesActuelles >= plan.getEtoilesCible() && relicPourComparaison >= plan.getRelicCible();
            if (ok) atteint++;

            String nomUnite = unitMap.getOrDefault(plan.getBaseId(), plan.getBaseId());
            details.add(new DetailRow(plan.getBaseId(), nomUnite, plan.getEtoilesCible(), plan.getRelicCible(), relicActuel, ok));
        }

        double pourcentage = 100.0 * atteint / plans.size();
        return new ExternalFarmProgress(atteint, plans.size(), pourcentage, details);
    }
}