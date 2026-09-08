package swgohManager.service;

import org.springframework.stereotype.Service;
import swgohManager.controller.dto.PlayerDatacronMecaniqueCheckProjection;
import swgohManager.controller.dto.PlayerDatacronStatSumProjection;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;

import java.math.BigDecimal;
import java.util.*;

/**
 * Logique commune de correspondance entre les datacrons cibles (PlanFarmDatacron) et les datacrons
 * physiques d'un ou plusieurs joueurs. Indépendante du fait qu'il s'agisse d'un joueur externe (index à 1 joueur)
 * ou de toute la guilde (index à N joueurs) : les clés d'index sont systématiquement préfixées par playerId.
 */
@Service
public class DatacronMatchingService {

    public record MecaniqueStatus(Integer tier, String description, boolean atteint) {}
    public record StatStatus(String statLibelle, BigDecimal valeurCible, BigDecimal valeurJoueur, boolean atteint) {}

    public record DatacronStatusJoueur(
            String playerId,
            List<MecaniqueStatus> mecaniques,
            List<StatStatus> stats,
            boolean tierMaxAtteint,
            boolean toutAtteint
    ) {}

    public record IndexDatacronsPhysiques(
            Map<String, Set<String>> mecaniquesParDatacron,          // clé "playerId|idDatacron" -> set "tier|abilityId"
            Map<String, Map<String, BigDecimal>> statsParDatacron,   // clé "playerId|idDatacron" -> map statType->valeur
            Map<String, Set<String>> datacronsParJoueurEtSet         // clé "playerId|setId" -> set idDatacron
    ) {}

    /** Construit l'index à partir des projections repository. Fonctionne pour 1 joueur (externe) ou N (guilde). */
    public IndexDatacronsPhysiques construireIndex(List<PlayerDatacronMecaniqueCheckProjection> mecaniquesRows,
                                                    List<PlayerDatacronStatSumProjection> statsRows) {
        Map<String, Set<String>> mecaniquesParDatacron = new HashMap<>();
        Map<String, Map<String, BigDecimal>> statsParDatacron = new HashMap<>();
        Map<String, Set<String>> datacronsParJoueurEtSet = new HashMap<>();

        for (PlayerDatacronMecaniqueCheckProjection p : mecaniquesRows) {
            String cleDatacron = p.getPlayerId() + "|" + p.getIdDatacron();
            mecaniquesParDatacron.computeIfAbsent(cleDatacron, k -> new HashSet<>())
                    .add(p.getTier() + "|" + p.getAbilityId());
            datacronsParJoueurEtSet.computeIfAbsent(p.getPlayerId() + "|" + p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        }

        for (PlayerDatacronStatSumProjection p : statsRows) {
            String cleDatacron = p.getPlayerId() + "|" + p.getIdDatacron();
            statsParDatacron.computeIfAbsent(cleDatacron, k -> new HashMap<>())
                    .put(p.getStatType(), p.getValue());
            datacronsParJoueurEtSet.computeIfAbsent(p.getPlayerId() + "|" + p.getSetId(), k -> new HashSet<>())
                    .add(p.getIdDatacron());
        }

        return new IndexDatacronsPhysiques(mecaniquesParDatacron, statsParDatacron, datacronsParJoueurEtSet);
    }

    /** Évalue un datacron cible pour UN joueur donné (interne ou externe), avec le détail par mécanique/stat. */
    public DatacronStatusJoueur evaluerDatacronPourJoueur(String playerId,
                                                           String setId,
                                                           List<PlanFarmDatacronMecanique> mecaniques,
                                                           List<PlanFarmDatacronStat> stats,
                                                           IndexDatacronsPhysiques index,
                                                           Map<String, String> descriptionParMecanique,
                                                           Map<String, String> libelleParStat) {

        Integer tierMax = mecaniques.stream().mapToInt(PlanFarmDatacronMecanique::getTier).max().orElse(0);
        PlanFarmDatacronMecanique mecTierMax = mecaniques.stream().filter(m -> m.getTier().equals(tierMax)).findFirst().orElse(null);

        Set<String> candidatsTmax = trouverCandidatsTmax(playerId, setId, mecTierMax, index);
        boolean tierMaxAtteint = !candidatsTmax.isEmpty();

        List<MecaniqueStatus> listMec = new ArrayList<>();
        for (PlanFarmDatacronMecanique mec : mecaniques) {
            boolean atteint = tierMaxAtteint && candidatsTmax.stream()
                    .anyMatch(idDatacron -> index.mecaniquesParDatacron()
                            .getOrDefault(playerId + "|" + idDatacron, Set.of())
                            .contains(mec.getTier() + "|" + mec.getAbilityId()));
            String desc = descriptionParMecanique.getOrDefault(mec.getTier() + "|" + mec.getAbilityId(), mec.getAbilityId());
            listMec.add(new MecaniqueStatus(mec.getTier(), desc, atteint));
        }

        List<StatStatus> listStat = new ArrayList<>();
        for (PlanFarmDatacronStat stat : stats) {
            if (stat.getStatValue() == null) continue;
            BigDecimal meilleureValeur = tierMaxAtteint ? candidatsTmax.stream()
                    .map(idDatacron -> index.statsParDatacron().getOrDefault(playerId + "|" + idDatacron, Map.of())
                            .getOrDefault(stat.getStatType(), BigDecimal.ZERO))
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO) : BigDecimal.ZERO;

            boolean atteint = meilleureValeur.compareTo(stat.getStatValue()) >= 0;
            String libelle = libelleParStat.getOrDefault(stat.getStatType(), stat.getStatType());
            listStat.add(new StatStatus(libelle, stat.getStatValue(), meilleureValeur, atteint));
        }

        boolean toutAtteint = tierMaxAtteint && candidatsTmax.stream()
                .anyMatch(idDatacron -> satisfaitTout(playerId, idDatacron, mecaniques, stats, index));

        return new DatacronStatusJoueur(playerId, listMec, listStat, tierMaxAtteint, toutAtteint);
    }

    private boolean satisfaitTout(String playerId, String idDatacron,
                                   List<PlanFarmDatacronMecanique> mecaniques,
                                   List<PlanFarmDatacronStat> stats,
                                   IndexDatacronsPhysiques index) {

        Set<String> mecPhysique = index.mecaniquesParDatacron().getOrDefault(playerId + "|" + idDatacron, Set.of());
        Map<String, BigDecimal> statPhysique = index.statsParDatacron().getOrDefault(playerId + "|" + idDatacron, Map.of());

        boolean toutesMecOk = mecaniques.stream()
                .allMatch(mec -> mecPhysique.contains(mec.getTier() + "|" + mec.getAbilityId()));

        boolean toutesStatsOk = stats.stream()
                .filter(s -> s.getStatValue() != null)
                .allMatch(stat -> statPhysique.getOrDefault(stat.getStatType(), BigDecimal.ZERO).compareTo(stat.getStatValue()) >= 0);

        return toutesMecOk && toutesStatsOk;
    }

    private Set<String> trouverCandidatsTmax(String playerId, String setId, PlanFarmDatacronMecanique mecTierMax, IndexDatacronsPhysiques index) {
        if (mecTierMax == null) return Set.of();
        String cleTmax = mecTierMax.getTier() + "|" + mecTierMax.getAbilityId();
        Set<String> datacronsDuJoueur = index.datacronsParJoueurEtSet().getOrDefault(playerId + "|" + setId, Set.of());

        Set<String> candidats = new HashSet<>();
        for (String idDatacron : datacronsDuJoueur) {
            if (index.mecaniquesParDatacron().getOrDefault(playerId + "|" + idDatacron, Set.of()).contains(cleTmax)) {
                candidats.add(idDatacron);
            }
        }
        return candidats;
    }
}