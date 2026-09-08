package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.model.Joueur;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanFarmDatacronProgressService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final PlayerDatacronAffixActuelRepository playerDatacronAffixActuelRepository;
    private final JoueurRepository joueurRepository;
    private final PlanFarmDatacronOptionsService optionsService;
    private final DatacronMatchingService datacronMatchingService;

    public record MecaniqueProgress(Long mecaniqueId, Integer tier, String description, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record StatProgress(Long statId, String statLibelle, BigDecimal valeurCible, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record DatacronProgress(Long id, String nom, List<MecaniqueProgress> mecaniques, List<StatProgress> stats, int joueursAtteint, int totalJoueurs, double pourcentage) {}
    public record SetProgress(String setId, List<DatacronProgress> datacrons, int joueursAtteint, int totalJoueurs, double pourcentage) {}

    public record JoueurDatacronStatus(String playerId, String playerName,
                                        List<DatacronMatchingService.MecaniqueStatus> mecaniques,
                                        List<DatacronMatchingService.StatStatus> stats,
                                        boolean tierMaxAtteint, boolean toutAtteint) {}
    public record DatacronDetail(Long id, String nom, String setId, Integer tierMax,
                                  List<JoueurDatacronStatus> joueursSansTierMax,
                                  List<JoueurDatacronStatus> joueursTierMaxSeul,
                                  List<JoueurDatacronStatus> joueursConformes) {}

    public List<SetProgress> construire() {
        List<Joueur> joueursActifs = joueurRepository.findAllByPresentInGuildTrue();
        int totalJoueurs = joueursActifs.size();
        if (totalJoueurs == 0) return List.of();

        DatacronMatchingService.IndexDatacronsPhysiques index = construireIndexGuilde();
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
                        EvaluationDatacron evaluation = evaluerDatacronPourGuilde(datacron, joueursActifs, index, descriptionParMecanique, libelleParStat);

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
        DatacronMatchingService.IndexDatacronsPhysiques index = construireIndexGuilde();
        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        chargerLibelles(descriptionParMecanique, libelleParStat);

        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(datacronId).stream()
                .sorted(Comparator.comparing(PlanFarmDatacronMecanique::getTier)).toList();
        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(datacronId);
        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);

        List<JoueurDatacronStatus> sansTierMax = new ArrayList<>();
        List<JoueurDatacronStatus> tierMaxSeul = new ArrayList<>();
        List<JoueurDatacronStatus> conformes = new ArrayList<>();

        for (Joueur j : joueursActifs) {
            DatacronMatchingService.DatacronStatusJoueur eval = datacronMatchingService.evaluerDatacronPourJoueur(
                    j.getPlayerId(), datacron.getSetId(), mecaniques, stats, index, descriptionParMecanique, libelleParStat);

            JoueurDatacronStatus statut = new JoueurDatacronStatus(
                    j.getPlayerId(), j.getPlayerName(), eval.mecaniques(), eval.stats(), eval.tierMaxAtteint(), eval.toutAtteint()
            );

            if (!eval.tierMaxAtteint()) sansTierMax.add(statut);
            else if (!eval.toutAtteint()) tierMaxSeul.add(statut);
            else conformes.add(statut);
        }

        String nomAffiche = (datacron.getNom() != null && !datacron.getNom().isBlank()) ? datacron.getNom() : "Datacron #" + datacron.getId();
        return new DatacronDetail(datacron.getId(), nomAffiche, datacron.getSetId(), tierMax, sansTierMax, tierMaxSeul, conformes);
    }

    private record EvaluationDatacron(String nomAffiche, List<MecaniqueProgress> mecaniqueProgresses,
                                       List<StatProgress> statProgresses, Map<String, Boolean> atteintParJoueur) {}

    private EvaluationDatacron evaluerDatacronPourGuilde(PlanFarmDatacron datacron, List<Joueur> joueursActifs,
                                                          DatacronMatchingService.IndexDatacronsPhysiques index,
                                                          Map<String, String> descriptionParMecanique,
                                                          Map<String, String> libelleParStat) {

        List<PlanFarmDatacronMecanique> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(datacron.getId());
        List<PlanFarmDatacronStat> stats = statRepository.findByPlanFarmDatacronId(datacron.getId());

        Map<String, Boolean> atteintParJoueur = new HashMap<>();
        Map<Long, Integer> joueursOkParMecanique = new HashMap<>();
        Map<Long, Integer> joueursOkParStat = new HashMap<>();
        for (PlanFarmDatacronMecanique mec : mecaniques) joueursOkParMecanique.put(mec.getId(), 0);
        for (PlanFarmDatacronStat stat : stats) joueursOkParStat.put(stat.getId(), 0);

        int totalJoueurs = joueursActifs.size();

        for (Joueur j : joueursActifs) {
            DatacronMatchingService.DatacronStatusJoueur eval = datacronMatchingService.evaluerDatacronPourJoueur(
                    j.getPlayerId(), datacron.getSetId(), mecaniques, stats, index, descriptionParMecanique, libelleParStat);

            for (int i = 0; i < mecaniques.size(); i++) {
                if (eval.mecaniques().get(i).atteint()) {
                    joueursOkParMecanique.merge(mecaniques.get(i).getId(), 1, Integer::sum);
                }
            }
            List<PlanFarmDatacronStat> statsAvecValeur = stats.stream().filter(s -> s.getStatValue() != null).toList();
            for (int i = 0; i < statsAvecValeur.size(); i++) {
                if (eval.stats().get(i).atteint()) {
                    joueursOkParStat.merge(statsAvecValeur.get(i).getId(), 1, Integer::sum);
                }
            }

            atteintParJoueur.put(j.getPlayerId(), eval.toutAtteint());
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

    private DatacronMatchingService.IndexDatacronsPhysiques construireIndexGuilde() {
        return datacronMatchingService.construireIndex(
                playerDatacronAffixActuelRepository.findMecaniquesEquipeesParJoueur(),
                playerDatacronAffixActuelRepository.findSommeStatsParJoueur()
        );
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