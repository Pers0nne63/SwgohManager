package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.model.*;
import swgohManager.repository.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Chargement commun des référentiels nécessaires au calcul de stats d'unité,
 * utilisé par les 4 services de calcul (actuel/objectif × interne/externe).
 */
@Service
@RequiredArgsConstructor
public class UnitStatReferentialService {

    private final UnitDefinitionRepository unitDefinitionRepository;
    private final UnitTierDefinitionRepository unitTierDefinitionRepository;
    private final StatProgressionRepository statProgressionRepository;
    private final RelicTierDefinitionRepository relicTierDefinitionRepository;
    private final MasteryStatRepository masteryStatRepository;
    private final StatDefinitionRepository statDefinitionRepository;

    public record UnitStatReferentiel(
            Map<String, UnitDefinition> unitDefinitions,
            Map<String, Map<Integer, Double>> statProgByProgId,
            Map<String, List<RelicTierDefinition>> relicByClasseTier,
            Map<String, Map<Integer, Double>> masteryByClasse,
            Map<Integer, Boolean> isDecimalByStat,
            Map<String, Map<Integer, Double>> tierStatsByUnitGear
    ) {}

    /** Charge le référentiel restreint aux unités demandées (perf : cas d'un seul joueur, interne ou externe). */
    public UnitStatReferentiel charger(List<String> definitionIds) {
        Map<String, UnitDefinition> unitDefinitions = unitDefinitionRepository.findByIdUnitIn(definitionIds).stream()
                .collect(Collectors.toMap(UnitDefinition::getIdUnit, u -> u));

        List<String> statProgIds = new ArrayList<>(unitDefinitions.values().stream()
                .map(UnitDefinition::getStatProgressionId).filter(Objects::nonNull).distinct().toList());

        List<RelicTierDefinition> relicDefsAll = relicTierDefinitionRepository.findAll();
        statProgIds.addAll(relicDefsAll.stream().map(RelicTierDefinition::getRelicStatTable)
                .filter(Objects::nonNull).distinct().toList());

        Map<String, Map<Integer, Double>> statProgByProgId = statProgressionRepository.findByStatProgressionIdIn(statProgIds).stream()
                .collect(Collectors.groupingBy(StatProgression::getStatProgressionId,
                        Collectors.toMap(StatProgression::getUnitStatId, sp -> sp.getValeur() / 100_000_000.0)));

        Map<String, List<RelicTierDefinition>> relicByClasseTier = relicDefsAll.stream()
                .filter(r -> r.getIdRelicTier() != null && r.getIdRelicTier().contains("_RELIC_TIER_"))
                .collect(Collectors.groupingBy(r ->
                        r.getIdRelicTier().substring(0, r.getIdRelicTier().indexOf("_RELIC_TIER_")) + "|" + r.getTierRelic()));

        Map<String, Map<Integer, Double>> masteryByClasse = masteryStatRepository.findAll().stream()
                .collect(Collectors.groupingBy(MasteryStat::getMasteryClass,
                        Collectors.toMap(MasteryStat::getUnitStatId, MasteryStat::getValue)));

        Map<Integer, Boolean> isDecimalByStat = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(StatDefinition::getStatId, sd -> Boolean.TRUE.equals(sd.getIsDecimal())));

        Map<String, Map<Integer, Double>> tierStatsByUnitGear = new HashMap<>();
        for (UnitTierDefinition t : unitTierDefinitionRepository.findByIdUnitIn(definitionIds)) {
            String cle = t.getIdUnit() + "|" + t.getGear();
            tierStatsByUnitGear.computeIfAbsent(cle, k -> new HashMap<>()).put(t.getStat(), t.getValeur() / 100_000_000.0);
        }

        return new UnitStatReferentiel(unitDefinitions, statProgByProgId, relicByClasseTier, masteryByClasse, isDecimalByStat, tierStatsByUnitGear);
    }

    /** Charge le référentiel complet (toutes les unités du jeu) — utilisé par le calcul d'objectifs, portée guilde entière. */
    public UnitStatReferentiel chargerComplet() {
        List<String> tousLesIds = unitDefinitionRepository.findAll().stream().map(UnitDefinition::getIdUnit).toList();
        return charger(tousLesIds);
    }
}