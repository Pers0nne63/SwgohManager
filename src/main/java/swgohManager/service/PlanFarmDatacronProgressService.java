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
import swgohManager.model.Joueur;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;

@Service
@RequiredArgsConstructor
public class PlanFarmDatacronProgressService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final PlayerDatacronAffixActuelRepository playerDatacronAffixActuelRepository;
    private final JoueurRepository joueurRepository;
    private final PlanFarmDatacronOptionsService optionsService;

    // ---- DTOs vue "liste des sets" (inchangés) ----
    public record MecaniqueProgress(Long mecaniqueId, Integer tier, String description, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record StatProgress(Long statId, String statLibelle, BigDecimal valeurCible, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record DatacronProgress(Long id, String nom, List<MecaniqueProgress> mecaniques, List<StatProgress> stats, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record SetProgress(String setId, List<DatacronProgress> datacrons, int joueursAtteint, int totalJoueurs, double pourcentage) {}

    // ---- DTOs vue "détail d'un datacron" (inchangés) ----
    public record JoueurMecaniqueStatus(Integer tier, String description, boolean atteint) {}
    public record JoueurStatStatus(String statLibelle, BigDecimal valeurCible, BigDecimal valeurJoueur, boolean atteint) {}
    public record JoueurDatacronStatus(String playerId, String playerName, List<JoueurMecaniqueStatus> mecaniques, List<JoueurStatStatus> stats, boolean tierMaxAtteint, boolean toutAtteint) {}
    public record DatacronDetail(Long id, String nom, String setId, Integer tierMax, List<JoueurDatacronStatus> joueursSansTierMax, List<JoueurDatacronStatus> joueursTierMaxSeul, List<JoueurDatacronStatus> joueursConformes) {}

    // ---- Index internes de calcul, reconstruits à chaque appel ----
    private record IndexJoueurDatacrons(
            Map<String, Set<String>> mecaniquesParDatacronPhysique,   // clé "playerId|idDatacron" -> set "tier|abilityId"
            Map<String, Map<String, BigDecimal>> statsParDatacronPhysique, // clé "playerId|idDatacron" -> map statType->valeur
            Map<String, Set<String>> datacronsParJoueurEtSet          // clé "playerId|setId" -> set idDatacron
    ) {}

    public List<SetProgress> construire() {
        List<Joueur> joueursActifs = joueurRepository.findAllByPresentInGuildTrue();
        int totalJoueurs = joueursActifs.size();
        if (totalJoueurs == 0) return List.of();

        IndexJoueurDatacrons index = construireIndex();
        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        chargerLibelles(descriptionParMecanique, libelleParStat);

        List<PlanFarmDatacron> datacronsCibles = planFarmDatacronRepository.findAll();
        Map<String, List<PlanFarmDatacron>> parSet = datacronsCibles.stream()
                .collect(Collectors.groupingBy(PlanFarmDatacron::getSetId, LinkedHashMap::new, Collectors.toList()));

        List<SetProgress> resultat = new ArrayList<>();

        parSet.entrySet().stream()
                .sorted(Map.Entry.<String, List<PlanFarmDatacron>>comparingByKey().reversed())
                .forEach(entry -> {
                    String setId = entry.getKey();
                    List<DatacronProgress> datacronProgresses = new ArrayList<>();
                    Map<String, Boolean> setConformeParJoueur = new HashMap<>();
                    for (Joueur j : joueursActifs) setConformeParJoueur.put(j.getPlayerId(), true);

                    for (PlanFarmDatacron datacron : entry.getValue()) {
                        EvaluationDatacron evaluation = evaluerDatacron(datacron, joueursActifs, index, descriptionParMecanique, libelleParStat);

                        for (Joueur j : joueursActifs) {
                            if (!evaluation.atteintParJoueur().getOrDefault(j.getPlayerId(), false)) {
                                setConformeParJoueur.put(j.getPlayerId(), false);
                            }
                        }

                        long joueursConformesDatacron = evaluation.atteintParJoueur().values().stream().filter(Boolean::booleanValue).count();
                        datacronProgresses.add(new DatacronProgress(
                                datacron.getId(), evaluation.nomAffiche(), evaluation.mecaniqueProgresses(), evaluation.statProgresses(),
                                (int) joueursConformesDatacron, totalJoueurs, pourcentage((int) joueursConformesDatacron, totalJoueurs)
                        ));
                    }

                    long joueursConformesSet = setConformeParJoueur.values().stream().filter(Boolean::booleanValue).count();
                    resultat.add(new SetProgress(setId, datacronProgresses, (int) joueursConformesSet, totalJoueurs, pourcentage((int) joueursConformesSet, totalJoueurs)));
                });

        return resultat;
    }

    public DatacronDetail construireDetail(Long datacronId) {
        PlanFarmDatacron datacron = planFarmDatacronRepository.findById(datacronId)
                .orElseThrow(() -> new IllegalArgumentException("Datacron cible introuvable : " + datacronId));

        List<Joueur> joueursActifs = joueurRepository.findAllByPresentInGuildTrue();
        IndexJoueurDatacrons index = construireIndex();
        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        chargerLibelles(descriptionParMecanique, libelleParStat);

        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(datacronId).stream()
                .sorted(Comparator.comparing(PlanFarmDatacronMecanique::getTier))
                .toList();
        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(datacronId);
        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);
        PlanFarmDatacronMecanique mecTierMax = mecaniques.stream().filter(m -> m.getTier().equals(tierMax)).findFirst().orElse(null);

        List<JoueurDatacronStatus> sansTierMax = new ArrayList<>();
        List<JoueurDatacronStatus> tierMaxSeul = new ArrayList<>();
        List<JoueurDatacronStatus> conformes = new ArrayList<>();

        for (Joueur j : joueursActifs) {
            Set<String> candidatsTmax = trouverCandidatsTmax(j.getPlayerId(), datacron.getSetId(), mecTierMax, index);
            boolean tierMaxAtteint = !candidatsTmax.isEmpty();

            List<JoueurMecaniqueStatus> statutsMecaniques = new ArrayList<>();
            for (PlanFarmDatacronMecanique mec : mecaniques) {
                boolean atteint = tierMaxAtteint && candidatsTmax.stream()
                        .anyMatch(idDatacron -> index.mecaniquesParDatacronPhysique()
                                .getOrDefault(j.getPlayerId() + "|" + idDatacron, Set.of())
                                .contains(mec.getTier() + "|" + mec.getAbilityId()));
                String description = descriptionParMecanique.getOrDefault(mec.getTier() + "|" + mec.getAbilityId(), mec.getAbilityId());
                statutsMecaniques.add(new JoueurMecaniqueStatus(mec.getTier(), description, atteint));
            }

            List<JoueurStatStatus> statutsStats = new ArrayList<>();
            for (PlanFarmDatacronStat stat : stats) {
                if (stat.getStatValue() == null) continue;
                BigDecimal meilleureValeur = tierMaxAtteint ? candidatsTmax.stream()
                        .map(idDatacron -> index.statsParDatacronPhysique().getOrDefault(j.getPlayerId() + "|" + idDatacron, Map.of())
                                .getOrDefault(stat.getStatType(), BigDecimal.ZERO))
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                boolean atteint = meilleureValeur.compareTo(stat.getStatValue()) >= 0;
                String statLibelle = libelleParStat.getOrDefault(stat.getStatType(), stat.getStatType());
                statutsStats.add(new JoueurStatStatus(statLibelle, stat.getStatValue(), meilleureValeur, atteint));
            }

            boolean toutSurUnMemeDatacron = tierMaxAtteint && candidatsTmax.stream().anyMatch(idDatacron ->
                    satisfaitTout(j.getPlayerId(), idDatacron, mecaniques, stats, index));

            JoueurDatacronStatus statut = new JoueurDatacronStatus(
                    j.getPlayerId(), j.getPlayerName(), statutsMecaniques, statutsStats, tierMaxAtteint, toutSurUnMemeDatacron
            );

            if (!tierMaxAtteint) sansTierMax.add(statut);
            else if (!toutSurUnMemeDatacron) tierMaxSeul.add(statut);
            else conformes.add(statut);
        }

        String nomAffiche = (datacron.getNom() != null && !datacron.getNom().isBlank()) ? datacron.getNom() : "Datacron #" + datacron.getId();
        return new DatacronDetail(datacron.getId(), nomAffiche, datacron.getSetId(), tierMax, sansTierMax, tierMaxSeul, conformes);
    }

    // ---------------------------------------------------------------------

    private record EvaluationDatacron(
            String nomAffiche,
            List<MecaniqueProgress> mecaniqueProgresses,
            List<StatProgress> statProgresses,
            Map<String, Boolean> atteintParJoueur
    ) {}

    private EvaluationDatacron evaluerDatacron(PlanFarmDatacron datacron,
                                                List<Joueur> joueursActifs,
                                                IndexJoueurDatacrons index,
                                                Map<String, String> descriptionParMecanique,
                                                Map<String, String> libelleParStat) {

        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(datacron.getId());
        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(datacron.getId());

        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);
        PlanFarmDatacronMecanique mecTierMax = mecaniques.stream().filter(m -> m.getTier().equals(tierMax)).findFirst().orElse(null);

        Map<String, Boolean> atteintParJoueur = new HashMap<>();
        Map<Long, Integer> joueursOkParMecanique = new HashMap<>();
        Map<Long, Integer> joueursOkParStat = new HashMap<>();
        for (PlanFarmDatacronMecanique mec : mecaniques) joueursOkParMecanique.put(mec.getId(), 0);
        for (PlanFarmDatacronStat stat : stats) joueursOkParStat.put(stat.getId(), 0);

        int totalJoueurs = joueursActifs.size();

        for (Joueur j : joueursActifs) {
            Set<String> candidatsTmax = trouverCandidatsTmax(j.getPlayerId(), datacron.getSetId(), mecTierMax, index);
            boolean tierMaxAtteint = !candidatsTmax.isEmpty();

            for (PlanFarmDatacronMecanique mec : mecaniques) {
                boolean atteint = tierMaxAtteint && candidatsTmax.stream()
                        .anyMatch(idDatacron -> index.mecaniquesParDatacronPhysique()
                                .getOrDefault(j.getPlayerId() + "|" + idDatacron, Set.of())
                                .contains(mec.getTier() + "|" + mec.getAbilityId()));
                if (atteint) joueursOkParMecanique.merge(mec.getId(), 1, Integer::sum);
            }

            for (PlanFarmDatacronStat stat : stats) {
                if (stat.getStatValue() == null) continue;
                boolean atteint = tierMaxAtteint && candidatsTmax.stream().anyMatch(idDatacron ->
                        index.statsParDatacronPhysique().getOrDefault(j.getPlayerId() + "|" + idDatacron, Map.of())
                                .getOrDefault(stat.getStatType(), BigDecimal.ZERO)
                                .compareTo(stat.getStatValue()) >= 0);
                if (atteint) joueursOkParStat.merge(stat.getId(), 1, Integer::sum);
            }

            boolean toutSurUnMemeDatacron = tierMaxAtteint && candidatsTmax.stream()
                    .anyMatch(idDatacron -> satisfaitTout(j.getPlayerId(), idDatacron, mecaniques, stats, index));
            atteintParJoueur.put(j.getPlayerId(), toutSurUnMemeDatacron);
        }

        List<MecaniqueProgress> mecaniqueProgresses = mecaniques.stream()
                .map(mec -> new MecaniqueProgress(
                        mec.getId(), mec.getTier(),
                        descriptionParMecanique.getOrDefault(mec.getTier() + "|" + mec.getAbilityId(), mec.getAbilityId()),
                        joueursOkParMecanique.get(mec.getId()), totalJoueurs,
                        pourcentage(joueursOkParMecanique.get(mec.getId()), totalJoueurs)
                ))
                .toList();

        List<StatProgress> statProgresses = stats.stream()
                .filter(s -> s.getStatValue() != null)
                .map(stat -> new StatProgress(
                        stat.getId(), libelleParStat.getOrDefault(stat.getStatType(), stat.getStatType()), stat.getStatValue(),
                        joueursOkParStat.get(stat.getId()), totalJoueurs,
                        pourcentage(joueursOkParStat.get(stat.getId()), totalJoueurs)
                ))
                .toList();

        String nomAffiche = (datacron.getNom() != null && !datacron.getNom().isBlank()) ? datacron.getNom() : "Datacron #" + datacron.getId();
        return new EvaluationDatacron(nomAffiche, mecaniqueProgresses, statProgresses, atteintParJoueur);
    }

    // Un même datacron physique satisfait-il TOUTES les mécaniques ET TOUTES les stats cibles ?
    private boolean satisfaitTout(String playerId, String idDatacron,
                                   List<PlanFarmDatacronMecanique> mecaniques,
                                   List<PlanFarmDatacronStat> stats,
                                   IndexJoueurDatacrons index) {

        Set<String> mecPhysique = index.mecaniquesParDatacronPhysique().getOrDefault(playerId + "|" + idDatacron, Set.of());
        Map<String, BigDecimal> statPhysique = index.statsParDatacronPhysique().getOrDefault(playerId + "|" + idDatacron, Map.of());

        boolean toutesMecOk = mecaniques.stream()
                .allMatch(mec -> mecPhysique.contains(mec.getTier() + "|" + mec.getAbilityId()));

        boolean toutesStatsOk = stats.stream()
                .filter(s -> s.getStatValue() != null)
                .allMatch(stat -> statPhysique.getOrDefault(stat.getStatType(), BigDecimal.ZERO).compareTo(stat.getStatValue()) >= 0);

        return toutesMecOk && toutesStatsOk;
    }

    // Datacrons physiques du joueur (même set) portant la mécanique du Tier max demandé
    private Set<String> trouverCandidatsTmax(String playerId, String setId, PlanFarmDatacronMecanique mecTierMax, IndexJoueurDatacrons index) {
        if (mecTierMax == null) return Set.of();
        String cleTmax = mecTierMax.getTier() + "|" + mecTierMax.getAbilityId();
        Set<String> datacronsDuJoueur = index.datacronsParJoueurEtSet().getOrDefault(playerId + "|" + setId, Set.of());

        Set<String> candidats = new HashSet<>();
        for (String idDatacron : datacronsDuJoueur) {
            if (index.mecaniquesParDatacronPhysique().getOrDefault(playerId + "|" + idDatacron, Set.of()).contains(cleTmax)) {
                candidats.add(idDatacron);
            }
        }
        return candidats;
    }

    private IndexJoueurDatacrons construireIndex() {
        Map<String, Set<String>> mecaniquesParDatacronPhysique = new HashMap<>();
        Map<String, Map<String, BigDecimal>> statsParDatacronPhysique = new HashMap<>();
        Map<String, Set<String>> datacronsParJoueurEtSet = new HashMap<>();

        playerDatacronAffixActuelRepository.findMecaniquesEquipeesParJoueur().forEach(p -> {
            String cleDatacron = p.getPlayerId() + "|" + p.getIdDatacron();
            mecaniquesParDatacronPhysique.computeIfAbsent(cleDatacron, k -> new HashSet<>())
                    .add(p.getTier() + "|" + p.getAbilityId());
            datacronsParJoueurEtSet.computeIfAbsent(p.getPlayerId() + "|" + p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        });

        playerDatacronAffixActuelRepository.findSommeStatsParJoueur().forEach(p -> {
            String cleDatacron = p.getPlayerId() + "|" + p.getIdDatacron();
            statsParDatacronPhysique.computeIfAbsent(cleDatacron, k -> new HashMap<>())
                    .put(p.getStatType(), p.getValue());
            datacronsParJoueurEtSet.computeIfAbsent(p.getPlayerId() + "|" + p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        });

        return new IndexJoueurDatacrons(mecaniquesParDatacronPhysique, statsParDatacronPhysique, datacronsParJoueurEtSet);
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

    private double pourcentage(int atteint, int total) {
        if (total == 0) return 0.0;
        return Math.round((atteint * 1000.0) / total) / 10.0;
    }
}