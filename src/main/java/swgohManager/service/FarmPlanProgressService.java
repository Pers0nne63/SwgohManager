package swgohManager.service;

import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.FarmPlan;
import swgohManager.repository.FarmPlanRepository;
import swgohManager.repository.RosterUnitActuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.controller.dto.RosterHistoryProgressProjection;
import swgohManager.model.SyncExecution;
import swgohManager.repository.RosterUnitHistoriqueRepository;
import swgohManager.repository.SyncExecutionRepository;
import java.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmPlanProgressService {

    private final FarmPlanRepository farmPlanRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final RosterUnitHistoriqueRepository rosterUnitHistoriqueRepository;
    private final SyncExecutionRepository syncExecutionRepository;

    public record DetailRow(String baseId, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}    public record PlayerFarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    
    
    public PlayerFarmProgress getProgression(String playerId) {
        return calculer(playerId, farmPlanRepository.findAll());
    }

    public Map<String, PlayerFarmProgress> getProgressionPourJoueurs(List<String> playerIds) {
        List<FarmPlan> plans = farmPlanRepository.findAll();
        Map<String, PlayerFarmProgress> resultat = new LinkedHashMap<>();
        for (String playerId : playerIds) {
            resultat.put(playerId, calculer(playerId, plans));
        }
        return resultat;
    }

    private PlayerFarmProgress calculer(String playerId, List<FarmPlan> plans) {
        if (plans.isEmpty()) {
            return new PlayerFarmProgress(0, 0, null, List.of());
        }

        Map<String, RosterBaseIdProgressProjection> parBaseId = rosterUnitActuelRepository
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

            details.add(new DetailRow(plan.getBaseId(), plan.getEtoilesCible(), plan.getRelicCible(), relicActuel, ok));
        }

        double pourcentage = 100.0 * atteint / plans.size();
        return new PlayerFarmProgress(atteint, plans.size(), pourcentage, details);
    }
    
    public record PointProgression(Instant date, Double pourcentage) {}

    public List<PointProgression> getProgressionDansLeTemps(String playerId) {
        List<FarmPlan> plans = farmPlanRepository.findAll();
        if (plans.isEmpty()) {
            return List.of();
        }

        List<RosterHistoryProgressProjection> lignes = rosterUnitHistoriqueRepository.findProgressionParSync(playerId);

        Map<Long, Map<String, RosterHistoryProgressProjection>> parSync = new TreeMap<>();
        for (RosterHistoryProgressProjection l : lignes) {
            parSync.computeIfAbsent(l.getIdSync(), k -> new HashMap<>()).put(l.getBaseId(), l);
        }

        if (parSync.isEmpty()) {
            return List.of();
        }

        Map<Long, Instant> dateParSync = new HashMap<>();
        for (SyncExecution se : syncExecutionRepository.findAllById(parSync.keySet())) {
            dateParSync.put(se.getIdSync(), se.getDateSync());
        }

        List<PointProgression> resultat = new ArrayList<>();
        for (Map.Entry<Long, Map<String, RosterHistoryProgressProjection>> entry : parSync.entrySet()) {
            Map<String, RosterHistoryProgressProjection> baseIdMap = entry.getValue();
            int atteint = 0;

            for (FarmPlan plan : plans) {
                RosterHistoryProgressProjection p = baseIdMap.get(plan.getBaseId());
                int etoiles = (p != null && p.getMaxEtoiles() != null) ? p.getMaxEtoiles() : 0;
                int relic = (p != null && p.getMaxRelic() != null) ? p.getMaxRelic() : 0;
                if (etoiles >= plan.getEtoilesCible() && relic >= plan.getRelicCible()) {
                    atteint++;
                }
            }

            Instant date = dateParSync.get(entry.getKey());
            double pourcentage = 100.0 * atteint / plans.size();
            resultat.add(new PointProgression(date, pourcentage));
        }

        return resultat;
    }
}