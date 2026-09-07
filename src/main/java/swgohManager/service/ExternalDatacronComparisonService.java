package swgohManager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.model.ExternalPlayerDatacronActuel;
import swgohManager.model.ExternalPlayerDatacronAffixActuel;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;

@Service
@RequiredArgsConstructor
public class ExternalDatacronComparisonService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;
    private final PlanFarmDatacronOptionsService optionsService;

    // ---- DTOs pour la vue du joueur externe ----
    public record ExternalDatacronMecaniqueStatus(
            Integer tier,
            String description,
            boolean atteint
    ) {}

    public record ExternalDatacronStatStatus(
            String statLibelle,
            BigDecimal valeurCible,
            BigDecimal valeurJoueur,
            boolean atteint
    ) {}

    public record ExternalDatacronStatus(
            Long planDatacronId,
            String nom,
            String setId,
            Integer tierMax,
            List<ExternalDatacronMecaniqueStatus> mecaniques,
            List<ExternalDatacronStatStatus> stats,
            boolean tierMaxAtteint,
            boolean toutAtteint
    ) {}

    public record ExternalSetDatacronProgress(
            String setId,
            List<ExternalDatacronStatus> datacrons,
            int totalCibles,
            int ciblesAtteintes,
            double pourcentage
    ) {}

    public record ExternalDatacronProgress(
            long totalDatacronsPhysiques,
            List<ExternalSetDatacronProgress> setsProgress
    ) {
        // --- Méthodes de compatibilité pour le template joueur-externe.html ---
        public record DetailDatacron(String nom, String setId, boolean atteint) {}

        public int atteint() {
            return setsProgress.stream().mapToInt(ExternalSetDatacronProgress::ciblesAtteintes).sum();
        }

        public int total() {
            return setsProgress.stream().mapToInt(ExternalSetDatacronProgress::totalCibles).sum();
        }

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

    // Index interne du joueur externe
    private record IndexDatacronsJoueur(
            Map<String, Set<String>> mecaniquesParDatacron, // idDatacron -> Set "tier|abilityId"
            Map<String, Map<String, BigDecimal>> statsParDatacron, // idDatacron -> Map statType->valeur
            Map<String, Set<String>> datacronsParSet // setId -> Set idDatacron
    ) {}

    public ExternalDatacronProgress comparer(String playerId) {
        List<ExternalPlayerDatacronActuel> datacronsJoueur = externalDatacronRepository.findByPlayerId(playerId);
        List<ExternalPlayerDatacronAffixActuel> affixesJoueur = externalAffixRepository.findByPlayerId(playerId);

        if (datacronsJoueur.isEmpty()) {
            return new ExternalDatacronProgress(0, List.of());
        }

        IndexDatacronsJoueur index = construireIndex(datacronsJoueur, affixesJoueur);

        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        chargerLibelles(descriptionParMecanique, libelleParStat);

        List<PlanFarmDatacron> datacronsCibles = planFarmDatacronRepository.findAll();
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
                        ExternalDatacronStatus status = evaluerDatacron(target, index, descriptionParMecanique, libelleParStat);
                        statusDatacrons.add(status);
                        if (status.toutAtteint()) {
                            ciblesAtteintes++;
                        }
                    }

                    int totalCibles = statusDatacrons.size();
                    double pct = totalCibles > 0 ? Math.round((ciblesAtteintes * 1000.0) / totalCibles) / 10.0 : 0.0;
                    setsProgress.add(new ExternalSetDatacronProgress(setId, statusDatacrons, totalCibles, ciblesAtteintes, pct));
                });

        return new ExternalDatacronProgress(datacronsJoueur.size(), setsProgress);
    }

    private ExternalDatacronStatus evaluerDatacron(PlanFarmDatacron target,
                                                  IndexDatacronsJoueur index,
                                                  Map<String, String> descriptionParMecanique,
                                                  Map<String, String> libelleParStat) {

        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(target.getId()).stream()
                .sorted(Comparator.comparing(PlanFarmDatacronMecanique::getTier))
                .toList();
        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(target.getId());

        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);
        PlanFarmDatacronMecanique mecTierMax = mecaniques.stream().filter(m -> m.getTier().equals(tierMax)).findFirst().orElse(null);

        Set<String> candidatsTmax = trouverCandidatsTmax(target.getSetId(), mecTierMax, index);
        boolean tierMaxAtteint = !candidatsTmax.isEmpty();

        List<ExternalDatacronMecaniqueStatus> listMec = new ArrayList<>();
        for (PlanFarmDatacronMecanique mec : mecaniques) {
            boolean atteint = tierMaxAtteint && candidatsTmax.stream()
                    .anyMatch(id -> index.mecaniquesParDatacron().getOrDefault(id, Set.of())
                            .contains(mec.getTier() + "|" + mec.getAbilityId()));
            String desc = descriptionParMecanique.getOrDefault(mec.getTier() + "|" + mec.getAbilityId(), mec.getAbilityId());
            listMec.add(new ExternalDatacronMecaniqueStatus(mec.getTier(), desc, atteint));
        }

        List<ExternalDatacronStatStatus> listStat = new ArrayList<>();
        for (PlanFarmDatacronStat stat : stats) {
            if (stat.getStatValue() == null) continue;
            BigDecimal meilleureValeur = tierMaxAtteint ? candidatsTmax.stream()
                    .map(id -> index.statsParDatacron().getOrDefault(id, Map.of()).getOrDefault(stat.getStatType(), BigDecimal.ZERO))
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;

            boolean atteint = meilleureValeur.compareTo(stat.getStatValue()) >= 0;
            String libelle = libelleParStat.getOrDefault(stat.getStatType(), stat.getStatType());
            listStat.add(new ExternalDatacronStatStatus(libelle, stat.getStatValue(), meilleureValeur, atteint));
        }

        boolean toutSurUnMemeDatacron = tierMaxAtteint && candidatsTmax.stream()
                .anyMatch(id -> satisfaitTout(id, mecaniques, stats, index));

        String nomAffiche = (target.getNom() != null && !target.getNom().isBlank()) ? target.getNom() : "Datacron #" + target.getId();

        return new ExternalDatacronStatus(
                target.getId(), nomAffiche, target.getSetId(), tierMax,
                listMec, listStat, tierMaxAtteint, toutSurUnMemeDatacron
        );
    }

    private boolean satisfaitTout(String idDatacron,
                                  List<PlanFarmDatacronMecanique> mecaniques,
                                  List<PlanFarmDatacronStat> stats,
                                  IndexDatacronsJoueur index) {

        Set<String> mecPhysique = index.mecaniquesParDatacron().getOrDefault(idDatacron, Set.of());
        Map<String, BigDecimal> statPhysique = index.statsParDatacron().getOrDefault(idDatacron, Map.of());

        boolean toutesMecOk = mecaniques.stream()
                .allMatch(mec -> mecPhysique.contains(mec.getTier() + "|" + mec.getAbilityId()));

        boolean toutesStatsOk = stats.stream()
                .filter(s -> s.getStatValue() != null)
                .allMatch(stat -> statPhysique.getOrDefault(stat.getStatType(), BigDecimal.ZERO).compareTo(stat.getStatValue()) >= 0);

        return toutesMecOk && toutesStatsOk;
    }

    private Set<String> trouverCandidatsTmax(String setId, PlanFarmDatacronMecanique mecTierMax, IndexDatacronsJoueur index) {
        if (mecTierMax == null) return Set.of();
        String cleTmax = mecTierMax.getTier() + "|" + mecTierMax.getAbilityId();
        Set<String> datacronsDuJoueur = index.datacronsParSet().getOrDefault(setId, Set.of());

        Set<String> candidats = new HashSet<>();
        for (String idDatacron : datacronsDuJoueur) {
            if (index.mecaniquesParDatacron().getOrDefault(idDatacron, Set.of()).contains(cleTmax)) {
                candidats.add(idDatacron);
            }
        }
        return candidats;
    }

    private IndexDatacronsJoueur construireIndex(List<ExternalPlayerDatacronActuel> datacrons, List<ExternalPlayerDatacronAffixActuel> affixes) {
        Map<String, Set<String>> mecaniquesParDatacron = new HashMap<>();
        Map<String, Map<String, BigDecimal>> statsParDatacron = new HashMap<>();
        Map<String, Set<String>> datacronsParSet = new HashMap<>();

        for (ExternalPlayerDatacronActuel d : datacrons) {
            if (d.getSetId() != null) {
                datacronsParSet.computeIfAbsent(String.valueOf(d.getSetId()), k -> new HashSet<>()).add(d.getIdDatacron());
            }
        }

        for (ExternalPlayerDatacronAffixActuel a : affixes) {
            String idDatacron = a.getIdDatacron();

            if (a.getAbilityId() != null && !a.getAbilityId().isBlank()) {
                int tier = a.getOrdre() != null ? a.getOrdre() : 0;
                mecaniquesParDatacron.computeIfAbsent(idDatacron, k -> new HashSet<>())
                        .add(tier + "|" + a.getAbilityId());
            }

            if (a.getStatType() != null && a.getStatValue() != null) {
                BigDecimal val = BigDecimal.valueOf(a.getStatValue());
                statsParDatacron.computeIfAbsent(idDatacron, k -> new HashMap<>())
                        .merge(String.valueOf(a.getStatType()), val, BigDecimal::add);
            }
        }

        return new IndexDatacronsJoueur(mecaniquesParDatacron, statsParDatacron, datacronsParSet);
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