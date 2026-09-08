package swgohManager.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.TbImportMapStat;
import swgohManager.controller.dto.TbImportPlayerStat;
import swgohManager.controller.dto.TbImportStatWrapper;
import swgohManager.model.TbImportActivite;
import swgohManager.model.TbImportScoreJoueur;
import swgohManager.model.TbPlanRound;
import swgohManager.model.TbPlaneteReference;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TbImportActiviteRepository;
import swgohManager.repository.TbImportScoreJoueurRepository;
import swgohManager.repository.TbPlanRoundRepository;
import swgohManager.repository.TbPlaneteReferenceRepository;
import swgohManager.repository.TerritoryBattleRepository;

@Service
@RequiredArgsConstructor
public class TbImportStatService {

	// Injection explicite pour éviter le conflit avec Lombok
	@org.springframework.beans.factory.annotation.Value("${app.import.directory:./imports}")
	private String importDirectory;
	
    private final ObjectMapper objectMapper;
    private final TbPlanRoundRepository planRoundRepository;
    private final TbPlaneteReferenceRepository planeteReferenceRepository;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbImportActiviteRepository tbImportActiviteRepository;
    private final TbImportScoreJoueurRepository tbImportScoreJoueurRepository;

    @Transactional
    public void importerStatsRound(String nomFichier, Long planId, Long territoryBattleId) throws IOException {
    	
        // 1. Récupérer la bataille de territoire (TB) en cours
        TerritoryBattle territoryBattle = territoryBattleRepository.findById(territoryBattleId)
                .orElseThrow(() -> new IllegalArgumentException("TerritoryBattle non trouvée pour l'ID : " + territoryBattleId));

        // 2. Déduire le numéro du round depuis le nom du fichier
        int roundNum = extraireRoundDuFichier(nomFichier);

        // 3. Récupérer la configuration du round pour le plan sélectionné
        TbPlanRound planRound = planRoundRepository.findByPlanIdAndRoundNum(planId, roundNum)
                .orElseThrow(() -> new IllegalArgumentException("Aucun round " + roundNum + " trouvé pour le plan " + planId));

        // 4. Récupérer les planètes actives pour ce round
        List<TbPlaneteReference> planetesActives = getPlanetesActives(planRound);

        // 5. Construire les suffixes attendus (_phaseXX_conflictYY[_bonus])
        List<String> suffixesAttendus = planetesActives.stream()
                .map(this::construireSuffixe)
                .toList();

        // 6. Lecture du fichier JSON
        File file = new File(importDirectory, nomFichier);
        TbImportStatWrapper root = objectMapper.readValue(file, TbImportStatWrapper.class);

        if (root != null && root.getCurrentStat() != null) {
            
            // 7. Filtrer et enregistrer dans les tables d'import
            for (TbImportMapStat stat : root.getCurrentStat()) {
                
                Optional<TbPlaneteReference> optRef = planetesActives.stream()
                        .filter(ref -> stat.getMapStatId() != null && stat.getMapStatId().contains(construireSuffixe(ref)))
                        .findFirst();

                if (optRef.isPresent()) {
                    TbPlaneteReference planeteRef = optRef.get();

                    // Création ou récupération de l'activité
                    TbImportActivite activite = tbImportActiviteRepository
                            .findByTerritoryBattleAndMapStatIdAndRoundNum(territoryBattle, stat.getMapStatId(), roundNum)
                            .orElseGet(() -> TbImportActivite.builder()
                                    .territoryBattle(territoryBattle)
                                    .mapStatId(stat.getMapStatId())
                                    .roundNum(roundNum)
                                    .build());

                    activite.setPhase(planeteRef.getPhase());
                    activite.setConflict(planeteRef.getConflict());
                    activite.setBonus(Boolean.TRUE.equals(planeteRef.getBonus()));
                    activite.setRoundNum(roundNum);
                    activite.setStatType(extraireStatType(stat.getMapStatId()));
                    activite.setCovertNum(extraireCovertNum(stat.getMapStatId()));

                    // On enregistre dans une NOUVELLE variable finale utilisable dans les lambdas
                    TbImportActivite activiteEnregistree = tbImportActiviteRepository.save(activite);

                    // Enregistrement des scores des joueurs dans TbImportScoreJoueur
                    if (stat.getPlayerStat() != null) {
                        for (TbImportPlayerStat pStat : stat.getPlayerStat()) {
                            Long scoreValue = parseLongSafe(pStat.getScore());

                            TbImportScoreJoueur scoreJoueur = tbImportScoreJoueurRepository
                                    .findByTbImportActiviteAndPlayerId(activiteEnregistree, pStat.getMemberId())
                                    .orElseGet(() -> TbImportScoreJoueur.builder()
                                            .tbImportActivite(activiteEnregistree)
                                            .playerId(pStat.getMemberId())
                                            .build());

                            scoreJoueur.setScore(scoreValue);
                            tbImportScoreJoueurRepository.save(scoreJoueur);
                        }
                    }
                }
            }
        }
    }

    private int extraireRoundDuFichier(String filename) {
        Matcher matcher = Pattern.compile("round(\\d+)").matcher(filename);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Impossible de déduire le round depuis le nom du fichier : " + filename);
    }

    private List<TbPlaneteReference> getPlanetesActives(TbPlanRound planRound) {
        List<Integer> idsActifs = Stream.of(
                planRound.getLsPlaneteId(),
                planRound.getDsPlaneteId(),
                planRound.getMixPlaneteId(),
                planRound.getZeffoPlaneteId(),
                planRound.getMandalorePlaneteId()
        ).filter(Objects::nonNull).map(Long::intValue).toList();

        return planeteReferenceRepository.findAllByPlaneteIdIn(idsActifs);
    }

    private String construireSuffixe(TbPlaneteReference ref) {
        String suffix = String.format("_phase%02d_conflict%02d", ref.getPhase(), ref.getConflict());
        if (Boolean.TRUE.equals(ref.getBonus())) {
            suffix += "_bonus";
        }
        return suffix;
    }

    private String extraireStatType(String mapStatId) {
        if (mapStatId == null) return null;
        if (mapStatId.contains("strike_encounter")) return "strike_encounter";
        if (mapStatId.contains("strike_attempt")) return "strike_attempt";
        if (mapStatId.contains("covert_attempt")) return "covert_attempt";
        if (mapStatId.contains("summary")) return "summary";
        if (mapStatId.contains("power")) return "power";

        int lastUnderscore = mapStatId.lastIndexOf('_');
        return (lastUnderscore != -1) ? mapStatId.substring(lastUnderscore + 1) : mapStatId;
    }

    private Integer extraireCovertNum(String mapStatId) {
        if (mapStatId == null) return null;
        Matcher matcher = Pattern.compile("covert_?(\\d+)").matcher(mapStatId);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    @PostConstruct
    public void initImportDirectory() {
        try {
            Path path = Paths.get(importDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Dossier d'import créé avec succès : " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier d'import : " + e.getMessage());
        }
    }
}