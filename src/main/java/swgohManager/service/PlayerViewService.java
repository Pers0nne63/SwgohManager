package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.model.PlayerModQActuel;
import swgohManager.model.PlayerRatingHistorique;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerRatingHistoriqueRepository;
import swgohManager.repository.RosterUnitModActuelRepository;

@Service
@RequiredArgsConstructor
public class PlayerViewService {

    private static final int ID_STAT_VITESSE = 5;
    private static final double DIVISEUR = 100_000_000.0;

    private final JoueurRepository joueurRepository;
    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final PlayerRatingHistoriqueRepository playerRatingHistoriqueRepository;
    private final RosterUnitModActuelRepository rosterUnitModActuelRepository;
    private final FarmPlanProgressService farmPlanProgressService;

    // DTO pour Chart.js (série de données pour une rareté donnée)
    public record ModSpeedDataset(
            String label,
            String backgroundColor,
            List<Long> data
    ) {}

    public record PlayerViewModel(
            Joueur joueur,
            PlayerModQActuel modQ,
            PlayerRatingHistorique ratingActuel,
            List<PlayerRatingHistorique> historiqueRating,
            List<Integer> vitesseLabels,
            List<ModSpeedDataset> vitesseDatasets,
            FarmPlanProgressService.PlayerFarmProgress farmPlan,
            List<String> farmPlanHistoLabels,
            List<Double> farmPlanHistoValues
    ) {}

    public PlayerViewModel construire(String playerId) {
        
        Joueur joueur = joueurRepository.findByPlayerId(playerId).orElse(null);
        PlayerModQActuel modQ = playerModQActuelRepository.findByPlayerId(playerId).orElse(null);

        List<PlayerRatingHistorique> historique = playerRatingHistoriqueRepository.findByPlayerIdOrderByDateReleveAsc(playerId);
        PlayerRatingHistorique ratingActuel = historique.isEmpty() ? null : historique.get(historique.size() - 1);

        List<RosterUnitModActuel> modsVitesse = rosterUnitModActuelRepository.findByPlayerIdAndIdSecondaire(playerId, ID_STAT_VITESSE);
        
        // Matrice 2D : [Rareté 0..6][Vitesse 0..31]
        long[][] compteurs = new long[7][32];

        for (RosterUnitModActuel m : modsVitesse) {
            if (m.getValeurSecondaire() == null) continue;
            
            int vitesse = (int) Math.round(m.getValeurSecondaire() / DIVISEUR);
            
            // Conversion sécurisée du String rarity en Integer
            Integer rarete = parseRarity(m.getRarity());

            if (vitesse >= 0 && vitesse <= 31 && rarete != null && rarete >= 1 && rarete <= 6) {
                compteurs[rarete][vitesse]++;
            }
        }

        // Labels X : 0 à 31
        List<Integer> labels = new ArrayList<>();
        for (int v = 0; v <= 31; v++) {
            labels.add(v);
        }

        // Palette de couleurs par rareté (1★ à 6★)
        Map<Integer, String> coulersRarete = Map.of(
            1, "#95a5a6", // 1* Gris
            2, "#2ecc71", // 2* Vert
            3, "#3498db", // 3* Bleu
            4, "#9b59b6", // 4* Violet
            5, "#e67e22", // 5* Orange / Gold 5*
            6, "#f1c40f"  // 6* Jaune / Gold 6*
        );

        // Construction des datasets pour Chart.js (un dataset par rareté)
        List<ModSpeedDataset> datasets = new ArrayList<>();
        for (int r = 1; r <= 6; r++) {
            List<Long> counts = new ArrayList<>();
            long totalRarite = 0;
            
            for (int v = 0; v <= 31; v++) {
                counts.add(compteurs[r][v]);
                totalRarite += compteurs[r][v];
            }
            
            // On n'ajoute la série que si le joueur possède au moins un mod de cette rareté
            if (totalRarite > 0) {
                datasets.add(new ModSpeedDataset(
                    r + "★",
                    coulersRarete.getOrDefault(r, "#333333"),
                    counts
                ));
            }
        }

        FarmPlanProgressService.PlayerFarmProgress farmPlan = farmPlanProgressService.getProgression(playerId);

        List<FarmPlanProgressService.PointProgression> historiqueFarm = farmPlanProgressService.getProgressionDansLeTemps(playerId);
        List<String> farmPlanHistoLabels = historiqueFarm.stream()
                .map(p -> p.date() != null
                        ? java.time.format.DateTimeFormatter.ofPattern("dd/MM").withZone(java.time.ZoneId.systemDefault()).format(p.date())
                        : "")
                .toList();
        List<Double> farmPlanHistoValues = historiqueFarm.stream().map(FarmPlanProgressService.PointProgression::pourcentage).toList();
        
        return new PlayerViewModel(joueur, modQ, ratingActuel, historique, labels, datasets, farmPlan, farmPlanHistoLabels, farmPlanHistoValues);
    }

    private Integer parseRarity(String rarityStr) {
        if (rarityStr == null || rarityStr.isBlank()) return null;
        try {
            return Integer.parseInt(rarityStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}