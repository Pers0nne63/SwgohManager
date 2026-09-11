package swgohManager.service;

import org.springframework.stereotype.Service;
import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.FarmPlan;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.UnitDefinitionRepository;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcul commun de la progression farm plan (étoiles/relic) à partir d'un roster,
 * qu'il s'agisse du roster d'un joueur interne ou externe.
 */
@Service
@RequiredArgsConstructor
public class FarmPlanCalculationService {

    private final UnitDefinitionRepository unitDefinitionRepository;

    public record DetailRow(String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}
    public record FarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}

    public FarmProgress calculer(List<FarmPlan> plans, List<RosterBaseIdProgressProjection> rosterProgress) {
        return calculer(plans, rosterProgress, chargerUnitMap());
    }

    /** Variante rapide : unitMap déjà chargé une fois pour tout le lot (synchro de masse). */
    public FarmProgress calculer(List<FarmPlan> plans, List<RosterBaseIdProgressProjection> rosterProgress, Map<String, String> unitMap) {
        if (plans.isEmpty()) {
            return new FarmProgress(0, 0, null, List.of());
        }

        Map<String, RosterBaseIdProgressProjection> parBaseId = rosterProgress.stream()
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
        return new FarmProgress(atteint, plans.size(), pourcentage, details);
    }

    private Map<String, String> chargerUnitMap() {
        return unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));
    }
}