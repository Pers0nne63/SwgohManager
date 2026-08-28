package swgohManager.service;

import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
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
public class RosterUnitStatObjectifService {

    private static final int COMBAT_TYPE_PERSONNAGE = 1;

    private final JoueurRepository joueurRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final UnitTierDefinitionRepository unitTierDefinitionRepository;
    private final StatProgressionRepository statProgressionRepository;
    private final RelicTierDefinitionRepository relicTierDefinitionRepository;
    private final MasteryStatRepository masteryStatRepository;
    private final LeaderboardModMoyRepository leaderboardModMoyRepository;
    private final RosterUnitStatObjectifRepository rosterUnitStatObjectifRepository;
    private final UnitStatFormulaService unitStatFormulaService;

    @Transactional
    public String calculerPourTousLesJoueurs() {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();

        // --- Référentiels communs, chargés une seule fois ---
        Map<String, UnitDefinition> unitDefinitions = unitDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(UnitDefinition::getIdUnit, u -> u));

        List<String> statProgIds = unitDefinitions.values().stream()
                .map(UnitDefinition::getStatProgressionId).filter(Objects::nonNull).distinct().toList();
        List<RelicTierDefinition> relicDefsAll = relicTierDefinitionRepository.findAll();
        List<String> relicStatTables = relicDefsAll.stream()
                .map(RelicTierDefinition::getRelicStatTable).filter(Objects::nonNull).distinct().toList();

        List<String> tousLesStatProgIds = new ArrayList<>(statProgIds);
        tousLesStatProgIds.addAll(relicStatTables);

        Map<String, Map<Integer, Double>> statProgByProgId = statProgressionRepository.findByStatProgressionIdIn(tousLesStatProgIds).stream()
                .collect(Collectors.groupingBy(StatProgression::getStatProgressionId,
                        Collectors.toMap(StatProgression::getUnitStatId, sp -> sp.getValeur() / 100_000_000.0)));

        Map<String, List<RelicTierDefinition>> relicByClasseTier = relicDefsAll.stream()
                .filter(r -> r.getIdRelicTier() != null && r.getIdRelicTier().contains("_RELIC_TIER_"))
                .collect(Collectors.groupingBy(r ->
                        r.getIdRelicTier().substring(0, r.getIdRelicTier().indexOf("_RELIC_TIER_")) + "|" + r.getTierRelic()));

        Map<String, Map<Integer, Double>> masteryByClasse = masteryStatRepository.findAll().stream()
                .collect(Collectors.groupingBy(MasteryStat::getMasteryClass,
                        Collectors.toMap(MasteryStat::getUnitStatId, MasteryStat::getValue)));

        Map<String, LeaderboardModMoy> modMoyByBaseId = leaderboardModMoyRepository.findAll().stream()
                .collect(Collectors.toMap(LeaderboardModMoy::getBaseId, m -> m));

        Map<String, Map<Integer, Double>> tierStatsByUnitGear = new HashMap<>();
        for (UnitTierDefinition t : unitTierDefinitionRepository.findAll()) {
            String cle = t.getIdUnit() + "|" + t.getGear();
            tierStatsByUnitGear.computeIfAbsent(cle, k -> new HashMap<>()).put(t.getStat(), t.getValeur() / 100_000_000.0);
        }

        int totalCalculees = 0, totalIgnorees = 0, totalSansReference = 0;

        for (Joueur joueur : joueurs) {
            List<RosterUnitActuel> unites = rosterUnitActuelRepository.findByPlayerId(joueur.getPlayerId());

            rosterUnitStatObjectifRepository.deleteByPlayerId(joueur.getPlayerId());
            rosterUnitStatObjectifRepository.flush();

            List<RosterUnitStatObjectif> resultats = new ArrayList<>();

            for (RosterUnitActuel u : unites) {
                UnitDefinition def = unitDefinitions.get(u.getDefinitionId());
                if (def == null || !Integer.valueOf(COMBAT_TYPE_PERSONNAGE).equals(def.getCombatType())
                        || def.getMasteryClass() == null || def.getPrimaryStat() == null
                        || u.getNiveau() == null || u.getGear() == null) {
                    totalIgnorees++;
                    continue;
                }

                LeaderboardModMoy modMoy = modMoyByBaseId.get(def.getBaseId());
                if (modMoy == null) {
                    totalSansReference++;
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

                ModAccumulator acc = mapVersAccumulator(modMoy);

                UnitStatResult r = unitStatFormulaService.calculerFinal(stats, u.getNiveau(), acc);

                resultats.add(mapVersObjectif(joueur.getPlayerId(), u.getIdUnit(), r));
                totalCalculees++;
            }

            rosterUnitStatObjectifRepository.saveAll(resultats);
        }

        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s), %d sans référence leaderboard",
                totalCalculees, totalIgnorees, totalSansReference);
        log.info(resultat);
        return resultat;
    }

    private ModAccumulator mapVersAccumulator(LeaderboardModMoy m) {
        ModAccumulator acc = new ModAccumulator();
        acc.speed = m.getSpeed(); acc.pSpeed = m.getPSpeed();
        acc.pOff = m.getPOff(); acc.fOff = m.getFOff();
        acc.pSante = m.getPSante(); acc.fSante = m.getFSante();
        acc.pProt = m.getPProt(); acc.fProt = m.getFProt();
        acc.pDef = m.getPDef(); acc.fDef = m.getFDef();
        acc.pot = m.getPot(); acc.ten = m.getTen();
        acc.cc = m.getCc(); acc.dc = m.getDc();
        acc.critAvoid = m.getCritAvoid(); acc.acc = m.getAcc();
        return acc;
    }

    private RosterUnitStatObjectif mapVersObjectif(String playerId, String idUnit, UnitStatResult r) {
        return RosterUnitStatObjectif.builder()
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
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            rosterUnitStatObjectifRepository.deleteByPlayerIdNotIn(joueursActifs);
            rosterUnitStatObjectifRepository.flush();
        }
    }
}