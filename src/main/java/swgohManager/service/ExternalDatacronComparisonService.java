package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalDatacronComparisonService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;
    private final PlanFarmDatacronOptionsService optionsService;
    private final DatacronMatchingService datacronMatchingService;

    public record ExternalDatacronStatus(
            Long planDatacronId, String nom, String setId, Integer tierMax,
            List<DatacronMatchingService.MecaniqueStatus> mecaniques,
            List<DatacronMatchingService.StatStatus> stats,
            boolean tierMaxAtteint, boolean toutAtteint
    ) {}

    public record ExternalSetDatacronProgress(String setId, List<ExternalDatacronStatus> datacrons,
                                               int totalCibles, int ciblesAtteintes, double pourcentage) {}

    public record ExternalDatacronProgress(long totalDatacronsPhysiques, List<ExternalSetDatacronProgress> setsProgress) {
        public record DetailDatacron(String nom, String setId, boolean atteint) {}

        public int atteint() { return setsProgress.stream().mapToInt(ExternalSetDatacronProgress::ciblesAtteintes).sum(); }
        public int total() { return setsProgress.stream().mapToInt(ExternalSetDatacronProgress::totalCibles).sum(); }
        public Double pourcentage() {
            int total = total();
            return total > 0 ? (100.0 * atteint() / total) : null;
        }
        public List<DetailDatacron> details() {
            return setsProgress.stream()
                    .flatMap(sp -> sp.datacrons().stream())
                    .map(d -> new DetailDatacron(d.nom(), d.setId(), d.toutAtteint()))
                    .toList();
        }
    }

    public ExternalDatacronProgress comparer(String playerId) {
    	long totalDatacronsPhysiques = externalDatacronRepository.findByPlayerId(playerId).size();

        DatacronMatchingService.IndexDatacronsPhysiques index = datacronMatchingService.construireIndex(
                externalAffixRepository.findMecaniquesEquipeesParJoueur(playerId),
                externalAffixRepository.findSommeStatsParJoueur(playerId)
        );

        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        chargerLibelles(descriptionParMecanique, libelleParStat);

        List<PlanFarmDatacron> datacronsCibles = planFarmDatacronRepository.findAll();
        if (datacronsCibles.isEmpty() || totalDatacronsPhysiques == 0) {
            return new ExternalDatacronProgress(totalDatacronsPhysiques, List.of());
        }

        Map<String, List<PlanFarmDatacron>> parSet = datacronsCibles.stream()
                .collect(Collectors.groupingBy(PlanFarmDatacron::getSetId, LinkedHashMap::new, Collectors.toList()));

        List<ExternalSetDatacronProgress> setsProgress = new ArrayList<>();

        parSet.entrySet().stream()
                .sorted(Map.Entry.<String, List<PlanFarmDatacron>>comparingByKey().reversed())
                .forEach(entry -> {
                    String setId = entry.getKey();
                    List<ExternalDatacronStatus> statusDatacrons = new ArrayList<>();
                    int ciblesAtteintes = 0;

                    for (PlanFarmDatacron target : entry.getValue()) {
                        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(target.getId())
                                .stream().sorted(Comparator.comparing(PlanFarmDatacronMecanique::getTier)).toList();
                        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(target.getId());
                        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);

                        DatacronMatchingService.DatacronStatusJoueur eval = datacronMatchingService.evaluerDatacronPourJoueur(
                                playerId, target.getSetId(), mecaniques, stats, index, descriptionParMecanique, libelleParStat);

                        String nomAffiche = (target.getNom() != null && !target.getNom().isBlank()) ? target.getNom() : "Datacron #" + target.getId();

                        ExternalDatacronStatus status = new ExternalDatacronStatus(
                                target.getId(), nomAffiche, target.getSetId(), tierMax,
                                eval.mecaniques(), eval.stats(), eval.tierMaxAtteint(), eval.toutAtteint()
                        );
                        statusDatacrons.add(status);
                        if (status.toutAtteint()) ciblesAtteintes++;
                    }

                    int totalCibles = statusDatacrons.size();
                    double pct = totalCibles > 0 ? Math.round((ciblesAtteintes * 1000.0) / totalCibles) / 10.0 : 0.0;
                    setsProgress.add(new ExternalSetDatacronProgress(setId, statusDatacrons, totalCibles, ciblesAtteintes, pct));
                });

        return new ExternalDatacronProgress(totalDatacronsPhysiques, setsProgress);
    }

    private void chargerLibelles(Map<String, String> descriptionParMecanique, Map<String, String> libelleParStat) {
        for (var setOption : optionsService.construire()) {
            for (var m : setOption.mecaniques()) {
                descriptionParMecanique.putIfAbsent(m.tier() + "|" + m.abilityId(), m.descriptionComplete());
            }
            for (var s : setOption.stats()) {
                libelleParStat.putIfAbsent(s.statType(), s.statLibelle());
            }
        }
    }
}