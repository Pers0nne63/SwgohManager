package swgohManager.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.GuildeRelicRepartitionProjection;
import swgohManager.model.Joueur;
import swgohManager.model.PlayerModQActuel;
import swgohManager.model.PlayerRatingHistorique;
import swgohManager.model.PlayerStatqActuel;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerRatingHistoriqueRepository;
import swgohManager.repository.PlayerStatqActuelRepository;
import swgohManager.repository.RosterUnitActuelRepository;
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
    private final PlayerStatqActuelRepository playerStatqActuelRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository; 
    private final FarmPlanIndProgressService farmPlanIndProgressService; 

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
            FarmPlanIndProgressService.PlayerFarmIndProgress farmPlanInd,
            List<String> farmPlanHistoLabels,
            List<Double> farmPlanHistoValues,
            Double statQ,
            GuildeRelicRepartitionProjection relicRepartition,
            Long nbMods5,   // 👈 nouveau
            Long nbMods6    // 👈 nouveau
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
            
            Integer rarete = parseRarity(m.getRarity());

            if (vitesse >= 0 && vitesse <= 31 && rarete != null && rarete >= 1 && rarete <= 6) {
                compteurs[rarete][vitesse]++;
            }
        }

        List<Integer> labels = new ArrayList<>();
        for (int v = 0; v <= 31; v++) {
            labels.add(v);
        }

        Map<Integer, String> coulersRarete = Map.of(
            1, "#95a5a6",
            2, "#2ecc71",
            3, "#3498db",
            4, "#9b59b6",
            5, "#e67e22",
            6, "#f1c40f"
        );

        List<ModSpeedDataset> datasets = new ArrayList<>();
        for (int r = 1; r <= 6; r++) {
            List<Long> counts = new ArrayList<>();
            long totalRarite = 0;
            
            for (int v = 0; v <= 31; v++) {
                counts.add(compteurs[r][v]);
                totalRarite += compteurs[r][v];
            }
            
            if (totalRarite > 0) {
                datasets.add(new ModSpeedDataset(
                    r + "★",
                    coulersRarete.getOrDefault(r, "#333333"),
                    counts
                ));
            }
        }

        FarmPlanProgressService.PlayerFarmProgress farmPlan = farmPlanProgressService.getProgressionPersistee(playerId);

        // 👇 Agrégation : un seul point (le dernier connu) par semaine ISO, au lieu d'un point par synchro
        List<FarmPlanProgressService.PointProgression> historiqueFarmBrut = farmPlanProgressService.getProgressionDansLeTempsPersistee(playerId);
        List<FarmPlanProgressService.PointProgression> historiqueFarm = aggregerDernierPointParSemaine(historiqueFarmBrut);

        List<String> farmPlanHistoLabels = historiqueFarm.stream()
                .map(p -> p.date() != null
                        ? java.time.format.DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault()).format(p.date())
                        : "")
                .toList();
        List<Double> farmPlanHistoValues = historiqueFarm.stream().map(FarmPlanProgressService.PointProgression::pourcentage).toList();
        
        Double statQ = playerStatqActuelRepository.findByPlayerId(playerId)
                .map(PlayerStatqActuel::getStatq).orElse(null);
        
        FarmPlanIndProgressService.PlayerFarmIndProgress farmPlanInd = farmPlanIndProgressService.getProgressionPersistee(playerId);
        
        GuildeRelicRepartitionProjection relicRepartition = rosterUnitActuelRepository.findRepartitionRelicsJoueur(playerId);

        // 👇 Nouveaux indicateurs : nombre de mods 5★ et 6★ (tous secondaires confondus)
        Long nbMods5 = rosterUnitModActuelRepository.countDistinctModsByPlayerIdAndRarity(playerId, "5");
        Long nbMods6 = rosterUnitModActuelRepository.countDistinctModsByPlayerIdAndRarity(playerId, "6");
        
        return new PlayerViewModel(joueur, modQ, ratingActuel, historique, labels, datasets, farmPlan, farmPlanInd,
                farmPlanHistoLabels, farmPlanHistoValues, statQ, relicRepartition, nbMods5, nbMods6);
    }

    private Integer parseRarity(String rarityStr) {
        if (rarityStr == null || rarityStr.isBlank()) return null;
        try {
            return Integer.parseInt(rarityStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Ne garde qu'un point par semaine ISO (le plus récent de chaque semaine),
     * en supposant que la liste d'entrée est déjà triée par date croissante.
     */
    private List<FarmPlanProgressService.PointProgression> aggregerDernierPointParSemaine(
            List<FarmPlanProgressService.PointProgression> points) {

        if (points == null || points.isEmpty()) return List.of();

        WeekFields wf = WeekFields.ISO;
        ZoneId zone = ZoneId.systemDefault();

        // LinkedHashMap : la clé "année-semaine" est insérée une seule fois (à la 1ère occurrence chronologique)
        // et sa valeur est écrasée à chaque nouveau point de la même semaine -> on garde le dernier en date,
        // tout en conservant l'ordre chronologique des semaines.
        Map<String, FarmPlanProgressService.PointProgression> dernierParSemaine = new LinkedHashMap<>();

        for (FarmPlanProgressService.PointProgression p : points) {
            if (p.date() == null) continue;
            Instant instant = p.date(); // 👈 hypothèse : PointProgression.date() renvoie un Instant
            LocalDate localDate = LocalDateTime.ofInstant(instant, zone).toLocalDate();
            int annee = localDate.get(wf.weekBasedYear());
            int semaine = localDate.get(wf.weekOfWeekBasedYear());
            String cle = annee + "-W" + String.format("%02d", semaine);

            dernierParSemaine.put(cle, p);
        }

        return new ArrayList<>(dernierParSemaine.values());
    }
}