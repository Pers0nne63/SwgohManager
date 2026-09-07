package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExternalPlayerDatacronPlanProgressService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalPlayerDatacronAffixActuelRepository;

    public record DetailDatacron(Long id, String nom, String setId, boolean atteint) {}
    public record ExternalDatacronProgress(int atteint, int total, Double pourcentage, List<DetailDatacron> details) {}

    private record IndexDatacronsJoueur(
            Map<String, Set<String>> mecaniquesParDatacron,
            Map<String, Map<String, BigDecimal>> statsParDatacron,
            Map<String, Set<String>> datacronsParSet
    ) {}

    @Transactional(readOnly = true)
    public ExternalDatacronProgress calculerProgressionGlobale(String playerId) {
        List<PlanFarmDatacron> datacronsCibles = planFarmDatacronRepository.findAll();
        if (datacronsCibles.isEmpty()) {
            return new ExternalDatacronProgress(0, 0, 0.0, List.of());
        }

        IndexDatacronsJoueur index = construireIndexJoueur(playerId);
        int totalAtteint = 0;
        List<DetailDatacron> details = new ArrayList<>();

        for (PlanFarmDatacron datacron : datacronsCibles) {
            List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(datacron.getId());
            List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(datacron.getId());

            Set<String> datacronsPhysiques = index.datacronsParSet().getOrDefault(datacron.getSetId(), Set.of());

            boolean atteint = datacronsPhysiques.stream().anyMatch(idDatacron ->
                    satisfaitTout(idDatacron, mecaniques, stats, index)
            );

            if (atteint) totalAtteint++;

            String nomAffiche = (datacron.getNom() != null && !datacron.getNom().isBlank())
                    ? datacron.getNom()
                    : "Datacron #" + datacron.getId();

            details.add(new DetailDatacron(datacron.getId(), nomAffiche, datacron.getSetId(), atteint));
        }

        int totalCibles = datacronsCibles.size();
        Double pourcentage = totalCibles > 0 ? (100.0 * totalAtteint / totalCibles) : 0.0;

        return new ExternalDatacronProgress(totalAtteint, totalCibles, pourcentage, details);
    }

    private boolean satisfaitTout(String idDatacron,
                                  List<PlanFarmDatacronMecanique> mecaniques,
                                  List<PlanFarmDatacronStat> stats,
                                  IndexDatacronsJoueur index) {

        Set<String> mecPhysiques = index.mecaniquesParDatacron().getOrDefault(idDatacron, Set.of());
        Map<String, BigDecimal> statPhysiques = index.statsParDatacron().getOrDefault(idDatacron, Map.of());

        boolean toutesMecOk = mecaniques.stream()
                .allMatch(mec -> mecPhysiques.contains(mec.getTier() + "|" + mec.getAbilityId()));

        boolean toutesStatsOk = stats.stream()
                .filter(s -> s.getStatValue() != null)
                .allMatch(stat -> statPhysiques.getOrDefault(stat.getStatType(), BigDecimal.ZERO).compareTo(stat.getStatValue()) >= 0);

        return toutesMecOk && toutesStatsOk;
    }

    private IndexDatacronsJoueur construireIndexJoueur(String playerId) {
        Map<String, Set<String>> mecaniquesParDatacron = new HashMap<>();
        Map<String, Map<String, BigDecimal>> statsParDatacron = new HashMap<>();
        Map<String, Set<String>> datacronsParSet = new HashMap<>();

        externalPlayerDatacronAffixActuelRepository.findMecaniquesEquipeesParJoueur(playerId).forEach(p -> {
            mecaniquesParDatacron.computeIfAbsent(p.getIdDatacron(), k -> new HashSet<>())
                    .add(p.getTier() + "|" + p.getAbilityId());
            datacronsParSet.computeIfAbsent(p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        });

        externalPlayerDatacronAffixActuelRepository.findSommeStatsParJoueur(playerId).forEach(p -> {
            statsParDatacron.computeIfAbsent(p.getIdDatacron(), k -> new HashMap<>())
                    .put(p.getStatType(), p.getValue());
            datacronsParSet.computeIfAbsent(p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        });

        return new IndexDatacronsJoueur(mecaniquesParDatacron, statsParDatacron, datacronsParSet);
    }
}