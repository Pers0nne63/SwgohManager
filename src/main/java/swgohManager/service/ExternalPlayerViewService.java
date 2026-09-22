package swgohManager.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.BaseIdLibelleProjection;
import swgohManager.controller.dto.GuildeRelicRepartitionProjection;
import swgohManager.controller.dto.StatQTeamProjection;
import swgohManager.model.ExternalPlayer;
import swgohManager.model.ExternalPlayerModQActuel;
import swgohManager.model.ExternalPlayerRaid;
import swgohManager.model.ExternalPlayerRatingActuel;
import swgohManager.model.ExternalPlayerStatqActuel;
import swgohManager.model.ExternalPlayerTbScore;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerRatingActuelRepository;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.repository.ExternalPlayerStatqActuelRepository;
import swgohManager.repository.ExternalPlayerStatqDetailActuelRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.ExternalRosterUnitModActuelRepository;
import swgohManager.repository.UnitDefinitionRepository;
import swgohManager.controller.dto.ExternalEraUnitProjection;
import swgohManager.repository.ExternalPlayerEraUnitStatusActuelRepository;

@Service
@RequiredArgsConstructor
public class ExternalPlayerViewService {

    private static final int ID_STAT_VITESSE = 5;
    private static final double DIVISEUR = 100_000_000.0;

    private final ExternalPlayerRepository externalPlayerRepository;
    private final ExternalPlayerRatingActuelRepository externalPlayerRatingActuelRepository;
    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalRosterUnitModActuelRepository externalRosterUnitModActuelRepository;
    private final FarmPlanProgressService farmPlanProgressService;
    private final ExternalOmicronComparisonService externalOmicronComparisonService;
    private final DatacronProgressService datacronProgressService;
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;
    private final ExternalTbStatsService externalTbStatsService;
    private final ExternalPlayerStatqActuelRepository externalPlayerStatqActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalPlayerStatqDetailActuelRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final ExternalPlayerEraUnitStatusActuelRepository externalPlayerEraUnitStatusActuelRepository;


    public record ModSpeedDataset(String label, String backgroundColor, List<Long> data) {}
    public record RelicBarSegment(String label, String cssColor, long count, double pourcentage) {}
    public record CollectionProgress(int possedes, int total, List<String> manquants) {}
    
    public record ExternalPlayerViewModel(
            ExternalPlayer joueur,
            ExternalPlayerRatingActuel rating,
            ExternalPlayerModQActuel modQ,
            ExternalPlayerStatqActuel statQ,
            List<StatQTeamProjection> statQDetails,
            List<Integer> vitesseLabels,
            List<ModSpeedDataset> vitesseDatasets,
            Long nbMods5,
            Long nbMods6,
            GuildeRelicRepartitionProjection relicRepartition,
            List<RelicBarSegment> relicBarSegments,
            Double relicMoyen,
            CollectionProgress legendProgress,
            CollectionProgress conqueteProgress,   
            FarmPlanProgressService.PlayerFarmProgress farmPlan,
            ExternalOmicronComparisonService.ExternalOmicronProgress omicronComparison,
            DatacronProgressService.UnJoueurDatacronProgress datacronComparison, 
            ExternalPlayerRaid raid,
            ExternalTbStatsService.TbSynthese tbSynthese,
            ExternalTbStatsService.MsStats tbMsStats,
            List<ExternalEraUnitProjection> eraUnits

    ) {}

    public ExternalPlayerViewModel construire(String playerId) {
        ExternalPlayer joueur = externalPlayerRepository.findByPlayerId(playerId).orElse(null);
        ExternalPlayerRatingActuel rating = externalPlayerRatingActuelRepository.findByPlayerId(playerId).orElse(null);
        ExternalPlayerModQActuel modQ = externalPlayerModQActuelRepository.findByPlayerId(playerId).orElse(null);

        List<ExternalRosterUnitModActuel> modsVitesse =
                externalRosterUnitModActuelRepository.findByPlayerIdAndIdSecondaire(playerId, ID_STAT_VITESSE);

        long[][] compteurs = new long[7][32];
        for (ExternalRosterUnitModActuel m : modsVitesse) {
            if (m.getValeurSecondaire() == null) continue;
            int vitesse = (int) Math.round(m.getValeurSecondaire() / DIVISEUR);
            Integer rarete = parseRarity(m.getRarity());
            if (vitesse >= 0 && vitesse <= 31 && rarete != null && rarete >= 1 && rarete <= 6) {
                compteurs[rarete][vitesse]++;
            }
        }

        List<Integer> labels = new ArrayList<>();
        for (int v = 0; v <= 31; v++) labels.add(v);

        Map<Integer, String> couleursRarete = Map.of(
                1, "#95a5a6", 2, "#2ecc71", 3, "#3498db",
                4, "#9b59b6", 5, "#e67e22", 6, "#f1c40f"
        );

        List<ModSpeedDataset> datasets = new ArrayList<>();
        for (int r = 1; r <= 6; r++) {
            List<Long> counts = new ArrayList<>();
            long total = 0;
            for (int v = 0; v <= 31; v++) {
                counts.add(compteurs[r][v]);
                total += compteurs[r][v];
            }
            if (total > 0) {
                datasets.add(new ModSpeedDataset(r + "★", couleursRarete.getOrDefault(r, "#333333"), counts));
            }
        }

        Long nbMods5 = externalRosterUnitModActuelRepository.countDistinctModsByPlayerIdAndRarity(playerId, "5");
        Long nbMods6 = externalRosterUnitModActuelRepository.countDistinctModsByPlayerIdAndRarity(playerId, "6");

        GuildeRelicRepartitionProjection relicRepartition =
                externalRosterUnitActuelRepository.findRepartitionRelicsJoueur(playerId);

        FarmPlanProgressService.PlayerFarmProgress farmPlan =
                farmPlanProgressService.getProgression(playerId, Portee.EXTERNE);

        ExternalOmicronComparisonService.ExternalOmicronProgress omicronComparison =
                externalOmicronComparisonService.comparer(playerId);

        DatacronProgressService.UnJoueurDatacronProgress datacronComparison =
                datacronProgressService.comparerPourJoueurExterne(playerId);
        
        ExternalPlayerRaid raid = externalPlayerRaidRepository.findByPlayerId(playerId).orElse(null);
        List<ExternalPlayerTbScore> tbLignes = externalPlayerTbScoreRepository.findByPlayerId(playerId);
        ExternalTbStatsService.TbSynthese tbSynthese = externalTbStatsService.calculerSynthese(tbLignes);
        ExternalTbStatsService.MsStats tbMsStats = externalTbStatsService.calculerMsStats(tbLignes);

        ExternalPlayerStatqActuel statQ = externalPlayerStatqActuelRepository
                .findByPlayerId(playerId)
                .orElse(null);

        List<StatQTeamProjection> statQDetails = externalPlayerStatqDetailActuelRepository
                .findStatQbyTeambyPlayerId(playerId);

        List<RelicBarSegment> relicBarSegments = construireRelicBarSegments(relicRepartition);
        Double relicMoyen = externalRosterUnitActuelRepository.findRelicMoyenJoueur(playerId);

        CollectionProgress legendProgress = construireCollectionProgress(
                unitDefinitionRepository.findDistinctLegendBaseIdsAvecLibelle(),
                externalRosterUnitActuelRepository.findBaseIdsLegendPossedes(playerId));

        CollectionProgress conqueteProgress = construireCollectionProgress(
                unitDefinitionRepository.findDistinctConqueteBaseIdsAvecLibelle(),
                externalRosterUnitActuelRepository.findBaseIdsConquetePossedes(playerId));
        
        List<ExternalEraUnitProjection> eraUnits =
                externalPlayerEraUnitStatusActuelRepository.findEraUnitsByPlayerId(playerId);


        return new ExternalPlayerViewModel(
                joueur, rating, modQ, statQ, statQDetails,
                labels, datasets, nbMods5, nbMods6,
                relicRepartition, relicBarSegments, relicMoyen, legendProgress, conqueteProgress,
                farmPlan, omicronComparison, datacronComparison,
                raid, tbSynthese, tbMsStats, eraUnits
                );
    }
    
    private List<RelicBarSegment> construireRelicBarSegments(GuildeRelicRepartitionProjection r) {
        if (r == null) return List.of();
        long relic10 = nvl(r.getRelic10());
        long relic9 = nvl(r.getRelic9());
        long relic8 = nvl(r.getRelic8());
        long relic67 = nvl(r.getRelic6Et7());
        long relic05 = nvl(r.getRelic0A5());
        long sansRelic = nvl(r.getSansRelic());
        long total = relic10 + relic9 + relic8 + relic67 + relic05 + sansRelic;

        return List.of(
            new RelicBarSegment("R10", "#a855f7", relic10, pct(relic10, total)),
            new RelicBarSegment("R9", "#ef4444", relic9, pct(relic9, total)),
            new RelicBarSegment("R8", "#f59e0b", relic8, pct(relic8, total)),
            new RelicBarSegment("R6-7", "#0ea5e9", relic67, pct(relic67, total)),
            new RelicBarSegment("R0-5", "#10b981", relic05, pct(relic05, total)),
            new RelicBarSegment("G1-13", "#64748b", sansRelic, pct(sansRelic, total))
        );
    }

    private long nvl(Long v) { return v != null ? v : 0L; }
    private double pct(long part, long total) { return total > 0 ? part * 100.0 / total : 0.0; }

    private CollectionProgress construireCollectionProgress(List<BaseIdLibelleProjection> tousLesBaseIds, List<String> baseIdsPossedes) {
        Set<String> possedesSet = new HashSet<>(baseIdsPossedes);
        List<String> manquants = tousLesBaseIds.stream()
                .filter(p -> !possedesSet.contains(p.getBaseId()))
                .map(BaseIdLibelleProjection::getLibelle)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new CollectionProgress(tousLesBaseIds.size() - manquants.size(), tousLesBaseIds.size(), manquants);
    }

    private Integer parseRarity(String rarityStr) {
        if (rarityStr == null || rarityStr.isBlank()) return null;
        try { return Integer.parseInt(rarityStr.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}