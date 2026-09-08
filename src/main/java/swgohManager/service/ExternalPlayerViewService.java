package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.GuildeRelicRepartitionProjection;
import swgohManager.controller.dto.StatQTeamProjection;
import swgohManager.model.ExternalPlayer;
import swgohManager.model.ExternalPlayerModQActuel;
import swgohManager.model.ExternalPlayerRaid;
import swgohManager.model.ExternalPlayerStatqActuel;
import swgohManager.model.ExternalPlayerStatqDetailActuel;
import swgohManager.model.ExternalPlayerTbScore;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.repository.ExternalPlayerStatqActuelRepository;
import swgohManager.repository.ExternalPlayerStatqDetailActuelRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.ExternalRosterUnitModActuelRepository;

@Service
@RequiredArgsConstructor
public class ExternalPlayerViewService {

    private static final int ID_STAT_VITESSE = 5;
    private static final double DIVISEUR = 100_000_000.0;

    private final ExternalPlayerRepository externalPlayerRepository;
    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalRosterUnitModActuelRepository externalRosterUnitModActuelRepository;
    private final ExternalFarmPlanComparisonService externalFarmPlanComparisonService;
    private final ExternalOmicronComparisonService externalOmicronComparisonService;
    private final ExternalDatacronComparisonService externalDatacronComparisonService;
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;
    private final ExternalTbStatsService externalTbStatsService;
    private final ExternalPlayerStatqActuelRepository externalPlayerStatqActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalPlayerStatqDetailActuelRepository;

    public record ModSpeedDataset(String label, String backgroundColor, List<Long> data) {}

    public record ExternalPlayerViewModel(
            ExternalPlayer joueur,
            ExternalPlayerModQActuel modQ,
            ExternalPlayerStatqActuel statQ,
            List<StatQTeamProjection> statQDetails,
            List<Integer> vitesseLabels,
            List<ModSpeedDataset> vitesseDatasets,
            Long nbMods5,
            Long nbMods6,
            GuildeRelicRepartitionProjection relicRepartition,
            ExternalFarmPlanComparisonService.ExternalFarmProgress farmPlan,
            ExternalOmicronComparisonService.ExternalOmicronProgress omicronComparison,
            ExternalDatacronComparisonService.ExternalDatacronProgress datacronComparison,
            ExternalPlayerRaid raid,
            ExternalTbStatsService.TbSynthese tbSynthese,
            ExternalTbStatsService.MsStats tbMsStats 
    ) {}

    public ExternalPlayerViewModel construire(String playerId) {
        ExternalPlayer joueur = externalPlayerRepository.findByPlayerId(playerId).orElse(null);
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

        // Comparaisons
        ExternalFarmPlanComparisonService.ExternalFarmProgress farmPlan =
                externalFarmPlanComparisonService.comparer(playerId);

        ExternalOmicronComparisonService.ExternalOmicronProgress omicronComparison =
                externalOmicronComparisonService.comparer(playerId);

        ExternalDatacronComparisonService.ExternalDatacronProgress datacronComparison =
                externalDatacronComparisonService.comparer(playerId);

        // Raids & TB
        ExternalPlayerRaid raid = externalPlayerRaidRepository.findByPlayerId(playerId).orElse(null);
        List<ExternalPlayerTbScore> tbLignes = externalPlayerTbScoreRepository.findByPlayerId(playerId);
        ExternalTbStatsService.TbSynthese tbSynthese = externalTbStatsService.calculerSynthese(tbLignes);
        ExternalTbStatsService.MsStats tbMsStats = externalTbStatsService.calculerMsStats(tbLignes);
        
        // StatQ 
        ExternalPlayerStatqActuel statQ = externalPlayerStatqActuelRepository
                .findByPlayerId(playerId)
                .orElse(null);

        // StatQ Details
        List<StatQTeamProjection> statQDetails = externalPlayerStatqDetailActuelRepository
                .findStatQbyTeambyPlayerId(playerId);

        return new ExternalPlayerViewModel(
                joueur, 
                modQ,
                statQ,
                statQDetails,
                labels, 
                datasets, 
                nbMods5, 
                nbMods6,
                relicRepartition, 
                farmPlan, 
                omicronComparison, 
                datacronComparison, 
                raid, 
                tbSynthese, 
                tbMsStats
        );
    }

    private Integer parseRarity(String rarityStr) {
        if (rarityStr == null || rarityStr.isBlank()) return null;
        try { return Integer.parseInt(rarityStr.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}