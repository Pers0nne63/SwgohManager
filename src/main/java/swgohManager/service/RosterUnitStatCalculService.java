package swgohManager.service;

import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
import swgohManager.util.ModAggregationUtil;
import swgohManager.util.ModAggregationUtil.ModAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitStatCalculService {

    private static final int COMBAT_TYPE_PERSONNAGE = 1;

    private final UnitDefinitionRepository unitDefinitionRepository;
    private final UnitTierDefinitionRepository unitTierDefinitionRepository;
    private final StatProgressionRepository statProgressionRepository;
    private final RelicTierDefinitionRepository relicTierDefinitionRepository;
    private final MasteryStatRepository masteryStatRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;
    private final UnitStatFormulaService unitStatFormulaService;

    @Transactional
    public String calculerEtEnregistrer(String playerId, List<RosterUnitActuel> unites, List<RosterUnitModActuel> mods) {
        List<String> definitionIds = unites.stream().map(RosterUnitActuel::getDefinitionId).distinct().toList();

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

        Map<String, List<RosterUnitModActuel>> modsParUnite = mods.stream()
                .collect(Collectors.groupingBy(RosterUnitModActuel::getIdUnit));

        rosterUnitStatActuelRepository.deleteByPlayerId(playerId);
        rosterUnitStatActuelRepository.flush();

        List<RosterUnitStatActuel> resultats = new ArrayList<>();
        int calculees = 0, ignorees = 0;

        for (RosterUnitActuel u : unites) {
            UnitDefinition def = unitDefinitions.get(u.getDefinitionId());
            if (def == null || !Integer.valueOf(COMBAT_TYPE_PERSONNAGE).equals(def.getCombatType())) {
                ignorees++;
                continue;
            }
            if (def.getMasteryClass() == null || def.getPrimaryStat() == null || u.getNiveau() == null || u.getGear() == null) {
                ignorees++;
                continue;
            }

            Map<Integer, Double> unitStats = tierStatsByUnitGear.getOrDefault(u.getDefinitionId() + "|" + u.getGear(), Map.of());
            Map<Integer, Double> statGrowth = statProgByProgId.getOrDefault(def.getStatProgressionId(), Map.of());

            Integer relicRawTier = u.getRelic() != null ? u.getRelic() + 2 : null;
            Map<Integer, Double> statRelicDefinition = Map.of();
            Map<Integer, Double> relicGrowth = Map.of();

            if (relicRawTier != null && relicRawTier >= 3) {
                List<RelicTierDefinition> lignes = relicByClasseTier.get(def.getMasteryClass() + "|" + relicRawTier);
                if (lignes != null && !lignes.isEmpty()) {
                    statRelicDefinition = lignes.stream()
                            .filter(l -> l.getStat() != null)
                            .collect(Collectors.toMap(RelicTierDefinition::getStat, l -> l.getValeur() / 100_000_000.0, (a, b) -> a));
                    String relicStatTable = lignes.get(0).getRelicStatTable();
                    if (relicStatTable != null) {
                        relicGrowth = statProgByProgId.getOrDefault(relicStatTable, Map.of());
                    }
                }
            }

            Map<Integer, Double> masteryStat = masteryByClasse.getOrDefault(def.getMasteryClass(), Map.of());

            double[] stats = unitStatFormulaService.calculerStatsDeBase(unitStats, statGrowth, statRelicDefinition,
                    relicGrowth, masteryStat, u.getNiveau(), def.getPrimaryStat());

            ModAccumulator acc = ModAggregationUtil.agregerMods(modsParUnite.getOrDefault(u.getIdUnit(), List.of()), isDecimalByStat);

            UnitStatResult r = unitStatFormulaService.calculerFinal(stats, u.getNiveau(), acc);

            resultats.add(mapVersActuel(playerId, u.getIdUnit(), r));
            calculees++;
        }

        rosterUnitStatActuelRepository.saveAll(resultats);

        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (vaisseaux/données manquantes)", calculees, ignorees);
        log.info("Stats calculées pour {} : {}", playerId, resultat);
        return resultat;
    }

    private RosterUnitStatActuel mapVersActuel(String playerId, String idUnit, UnitStatResult r) {
        return RosterUnitStatActuel.builder()
                .playerId(playerId).idUnit(idUnit)
                .sante(r.sante()).protection(r.protection()).vitesse(r.vitesse())
                .attaquePhysique(r.attaquePhysique()).attaqueSpeciale(r.attaqueSpeciale())
                .armure(r.armure()).resistance(r.resistance())
                .penetrationArmure(r.penetrationArmure()).penetrationResistance(r.penetrationResistance())
                .esquive(r.esquive()).deviation(r.deviation())
                .ccPhysique(r.ccPhysique()).ccSpeciaux(r.ccSpeciaux())
                .degatsCritiques(r.degatsCritiques()).pouvoir(r.pouvoir()).tenacite(r.tenacite())
                .volDeSante(r.volDeSante())
                .precisionPhysique(r.precisionPhysique()).precisionSpeciale(r.precisionSpeciale())
                .esquiveCritiquePhysique(r.esquiveCritiquePhysique()).esquiveCritiqueSpeciale(r.esquiveCritiqueSpeciale())
                .defense(r.defense())
                .build();
    }
}