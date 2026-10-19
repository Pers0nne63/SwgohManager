package swgohManager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.LeaderboardMod;
import swgohManager.model.LeaderboardModMoy;
import swgohManager.model.LeaderboardUnit;
import swgohManager.model.StatDefinition;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.LeaderboardModMoyRepository;
import swgohManager.repository.LeaderboardModRepository;
import swgohManager.repository.LeaderboardUnitRepository;
import swgohManager.repository.StatDefinitionRepository;
import swgohManager.repository.UnitDefinitionRepository;
import swgohManager.util.ModAggregationUtil;
import swgohManager.util.ModAggregationUtil.ModAccumulator;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardModMoyService {

    private final LeaderboardUnitRepository leaderboardUnitRepository;
    private final LeaderboardModRepository leaderboardModRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final LeaderboardModMoyRepository leaderboardModMoyRepository;
    private final EntityManager entityManager;

    @Transactional
    public String calculerMoyennes() {
        List<LeaderboardUnit> unites = leaderboardUnitRepository.findAll();   // ~85 000 lignes, OK
        List<String> definitionIds = unites.stream().map(LeaderboardUnit::getDefinitionId).distinct().toList();

        Map<String, String> baseIdParDefinitionId = unitDefinitionRepository.findByIdUnitIn(definitionIds).stream()
                .collect(Collectors.toMap(UnitDefinition::getIdUnit, UnitDefinition::getBaseId));

        Map<Integer, Boolean> isDecimalByStat = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(StatDefinition::getStatId, sd -> Boolean.TRUE.equals(sd.getIsDecimal())));

        Map<String, List<LeaderboardUnit>> unitesParJoueur = unites.stream()
                .collect(Collectors.groupingBy(LeaderboardUnit::getPlayerId));

        Map<String, List<ModAccumulator>> accParBaseId = new HashMap<>();

        for (Map.Entry<String, List<LeaderboardUnit>> joueur : unitesParJoueur.entrySet()) {
            // Uniquement les mods de ce joueur (~8 000 lignes au lieu de 1,6 M)
            Map<String, List<LeaderboardMod>> modsParUnite = leaderboardModRepository
                    .findByPlayerId(joueur.getKey()).stream()
                    .collect(Collectors.groupingBy(LeaderboardMod::getIdUnit));

            for (LeaderboardUnit u : joueur.getValue()) {
                String baseId = baseIdParDefinitionId.get(u.getDefinitionId());
                if (baseId == null) continue;

                List<LeaderboardMod> modsUnite = modsParUnite.getOrDefault(u.getIdUnit(), List.of());
                ModAccumulator acc = ModAggregationUtil.agregerMods(modsUnite, isDecimalByStat);
                accParBaseId.computeIfAbsent(baseId, k -> new ArrayList<>()).add(acc);
            }

            entityManager.clear();   // libère les entités du joueur avant le suivant
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