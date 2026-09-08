package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.RelicTierDefinition;
import swgohManager.model.UnitDefinition;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
import swgohManager.service.UnitStatReferentialService.UnitStatReferentiel;
import swgohManager.util.ModAggregationUtil.ModAccumulator;

/**
 * Calcul commun des stats finales d'une unité, utilisé par les 4 services
 * (actuel/objectif × interne/externe). Seule la source du ModAccumulator diffère selon l'appelant.
 */
@Service
@RequiredArgsConstructor
public class UnitStatCalculationService {

    private static final int COMBAT_TYPE_PERSONNAGE = 1;

    private final UnitStatFormulaService unitStatFormulaService;

    public record UnitCalculResult(String idUnit, UnitStatResult stats) {}

    /** Renvoie null si l'unité doit être ignorée (vaisseau, données manquantes). */
    public UnitStatResult calculerUnite(UnitDefinition def, Integer niveau, Integer gear, Integer relic,
                                         UnitStatReferentiel ref, ModAccumulator acc) {
        if (def == null || !Integer.valueOf(COMBAT_TYPE_PERSONNAGE).equals(def.getCombatType())) return null;
        if (def.getMasteryClass() == null || def.getPrimaryStat() == null || niveau == null || gear == null) return null;

        Map<Integer, Double> unitStats = ref.tierStatsByUnitGear().getOrDefault(def.getIdUnit() + "|" + gear, Map.of());
        Map<Integer, Double> statGrowth = ref.statProgByProgId().getOrDefault(def.getStatProgressionId(), Map.of());

        Integer relicRawTier = relic != null ? relic + 2 : null;
        Map<Integer, Double> statRelicDefinition = Map.of();
        Map<Integer, Double> relicGrowth = Map.of();

        if (relicRawTier != null && relicRawTier >= 3) {
            List<RelicTierDefinition> lignes = ref.relicByClasseTier().get(def.getMasteryClass() + "|" + relicRawTier);
            if (lignes != null && !lignes.isEmpty()) {
                statRelicDefinition = lignes.stream()
                        .filter(l -> l.getStat() != null)
                        .collect(Collectors.toMap(RelicTierDefinition::getStat, l -> l.getValeur() / 100_000_000.0, (a, b) -> a));
                String relicStatTable = lignes.get(0).getRelicStatTable();
                if (relicStatTable != null) {
                    relicGrowth = ref.statProgByProgId().getOrDefault(relicStatTable, Map.of());
                }
            }
        }

        Map<Integer, Double> masteryStat = ref.masteryByClasse().getOrDefault(def.getMasteryClass(), Map.of());

        double[] stats = unitStatFormulaService.calculerStatsDeBase(unitStats, statGrowth, statRelicDefinition,
                relicGrowth, masteryStat, niveau, def.getPrimaryStat());

        return unitStatFormulaService.calculerFinal(stats, niveau, acc);
    }

    /**
     * Calcule les stats pour un ensemble d'unités pivot. Le fournisseur de ModAccumulator reçoit
     * l'unité pivot ET sa UnitDefinition (utile pour retrouver le baseId côté calcul d'objectifs).
     * Si le fournisseur renvoie null (ex : pas de référence leaderboard pour ce baseId), l'unité est ignorée.
     */
    public List<UnitCalculResult> calculerPourUnites(List<UniteStatInput> unites, UnitStatReferentiel ref,
                                                      BiFunction<UniteStatInput, UnitDefinition, ModAccumulator> accumulatorFournisseur) {
        List<UnitCalculResult> resultats = new ArrayList<>();
        for (UniteStatInput u : unites) {
            UnitDefinition def = ref.unitDefinitions().get(u.definitionId());
            if (def == null) continue;

            ModAccumulator acc = accumulatorFournisseur.apply(u, def);
            if (acc == null) continue;

            UnitStatResult r = calculerUnite(def, u.niveau(), u.gear(), u.relic(), ref, acc);
            if (r != null) resultats.add(new UnitCalculResult(u.idUnit(), r));
        }
        return resultats;
    }
}