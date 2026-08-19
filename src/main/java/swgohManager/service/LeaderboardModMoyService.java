package swgohManager.service;

import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.util.ModAggregationUtil;
import swgohManager.util.ModAggregationUtil.ModAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardModMoyService {

    private final LeaderboardUnitRepository leaderboardUnitRepository;
    private final LeaderboardModRepository leaderboardModRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final LeaderboardModMoyRepository leaderboardModMoyRepository;

    @Transactional
    public String calculerMoyennes() {
        List<LeaderboardUnit> unites = leaderboardUnitRepository.findAll();
        List<String> definitionIds = unites.stream().map(LeaderboardUnit::getDefinitionId).distinct().toList();

        Map<String, String> baseIdParDefinitionId = unitDefinitionRepository.findByIdUnitIn(definitionIds).stream()
                .collect(Collectors.toMap(UnitDefinition::getIdUnit, UnitDefinition::getBaseId));

        Map<String, List<LeaderboardMod>> modsParUnite = leaderboardModRepository.findAll().stream()
                .collect(Collectors.groupingBy(m -> m.getPlayerId() + "|" + m.getIdUnit()));

        Map<Integer, Boolean> isDecimalByStat = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(StatDefinition::getStatId, sd -> Boolean.TRUE.equals(sd.getIsDecimal())));

        Map<String, List<ModAccumulator>> accParBaseId = new HashMap<>();

        for (LeaderboardUnit u : unites) {
            String baseId = baseIdParDefinitionId.get(u.getDefinitionId());
            if (baseId == null) continue;

            List<LeaderboardMod> modsUnite = modsParUnite.getOrDefault(u.getPlayerId() + "|" + u.getIdUnit(), List.of());
            ModAccumulator acc = ModAggregationUtil.agregerMods(modsUnite, isDecimalByStat);
            accParBaseId.computeIfAbsent(baseId, k -> new ArrayList<>()).add(acc);
        }

        leaderboardModMoyRepository.deleteAll();
        leaderboardModMoyRepository.flush();

        List<LeaderboardModMoy> resultats = new ArrayList<>();
        for (Map.Entry<String, List<ModAccumulator>> entry : accParBaseId.entrySet()) {
            List<ModAccumulator> accs = entry.getValue();
            resultats.add(LeaderboardModMoy.builder()
                    .baseId(entry.getKey())
                    .nbEchantillons(accs.size())
                    .speed(moy(accs, a -> a.speed)).pSpeed(moy(accs, a -> a.pSpeed))
                    .pOff(moy(accs, a -> a.pOff)).fOff(moy(accs, a -> a.fOff))
                    .pSante(moy(accs, a -> a.pSante)).fSante(moy(accs, a -> a.fSante))
                    .pProt(moy(accs, a -> a.pProt)).fProt(moy(accs, a -> a.fProt))
                    .pDef(moy(accs, a -> a.pDef)).fDef(moy(accs, a -> a.fDef))
                    .pot(moy(accs, a -> a.pot)).ten(moy(accs, a -> a.ten))
                    .cc(moy(accs, a -> a.cc)).dc(moy(accs, a -> a.dc))
                    .critAvoid(moy(accs, a -> a.critAvoid)).acc(moy(accs, a -> a.acc))
                    .build());
        }

        leaderboardModMoyRepository.saveAll(resultats);

        String resultat = String.format("%d baseId(s) avec moyenne de mods calculée", resultats.size());
        log.info(resultat);
        return resultat;
    }

    private double moy(List<ModAccumulator> accs, ToDoubleFunction<ModAccumulator> f) {
        return accs.stream().mapToDouble(f).average().orElse(0);
    }
}