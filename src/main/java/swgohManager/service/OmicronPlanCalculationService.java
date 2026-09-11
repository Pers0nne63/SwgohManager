package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.UnitDefinitionRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcul commun de la progression omicron (par priorité, colonnes, statut détaillé),
 * utilisé par la synchro/vue interne (OmicronPlanProgressService) et externe (ExternalPlayerOmicronPlanProgressService).
 * Ne dépend que de PlayerOmicronStatusProjection, renvoyé identiquement par les deux repositories.
 */
@Service
@RequiredArgsConstructor
public class OmicronPlanCalculationService {

    private final UnitDefinitionRepository unitDefinitionRepository;
    private final OmicronPlanService omicronPlanService;

    public record DetailRow(String baseId, String label, boolean atteint) {}
    public record PrioriteSummary(int priorite, int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PlayerOmicronProgress(Map<Integer, PrioriteSummary> parPriorite) {}
    public record GlobalSummary(String label, int atteint, int total, Double pourcentage) {}
    public record OmicronColonneDetail(String cle, String baseId, String idSkill, String label, int priorite) {}

    public Map<String, String> buildUnitMap() {
        return unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));
    }

    public Map<String, OmicronPlanService.Option> buildOptionsMap() {
        Map<String, OmicronPlanService.Option> map = new HashMap<>();
        for (OmicronPlanService.Option o : omicronPlanService.getOptionsDisponibles()) {
            map.put(o.baseId() + "|" + o.idSkill(), o);
        }
        return map;
    }

    private String buildLabel(String baseId, String cle, Map<String, String> unitMap, Map<String, OmicronPlanService.Option> optionsMap) {
        String nomUnite = unitMap.getOrDefault(baseId, baseId);
        OmicronPlanService.Option option = optionsMap.get(cle);
        return option != null ? option.label().replace(baseId, nomUnite) : nomUnite;
    }

    /** Statut brut (clé "baseId|idSkill" -> omicronApplied) à partir des lignes retournées par le repository (interne ou externe). */
    public Map<String, Boolean> statutParCle(List<PlayerOmicronStatusProjection> statutRows) {
        Map<String, Boolean> statutJoueur = new HashMap<>();
        for (PlayerOmicronStatusProjection p : statutRows) {
            statutJoueur.put(p.getBaseId() + "|" + p.getIdSkill(), Boolean.TRUE.equals(p.getOmicronApplied()));
        }
        return statutJoueur;
    }

    /** Progression par priorité (1 à 4) pour un joueur, à partir des plans cibles et de son statut omicron. */
    public PlayerOmicronProgress calculerProgression(List<OmicronPlan> plans, List<PlayerOmicronStatusProjection> statutRows) {
        return calculerProgression(plans, statutRows, buildUnitMap(), buildOptionsMap());
    }

    /** Variante rapide : unitMap/optionsMap déjà chargés une fois pour tout le lot (synchro de masse). */
    public PlayerOmicronProgress calculerProgression(List<OmicronPlan> plans, List<PlayerOmicronStatusProjection> statutRows,
            Map<String, String> unitMap, Map<String, OmicronPlanService.Option> optionsMap) {
        Map<String, Boolean> statutJoueur = statutParCle(statutRows);

        Map<Integer, List<OmicronPlan>> parPrioriteBrut = new TreeMap<>();
        for (OmicronPlan plan : plans) {
            int p = plan.getPriorite() != null ? plan.getPriorite() : 0;
            parPrioriteBrut.computeIfAbsent(p, k -> new ArrayList<>()).add(plan);
        }

        Map<Integer, PrioriteSummary> resultat = new LinkedHashMap<>();
        for (int priorite = 1; priorite <= 4; priorite++) {
            List<OmicronPlan> lignes = parPrioriteBrut.getOrDefault(priorite, List.of());
            List<DetailRow> details = new ArrayList<>();
            int atteint = 0;

            for (OmicronPlan plan : lignes) {
                String cle = plan.getBaseId() + "|" + plan.getIdSkill();
                boolean ok = Boolean.TRUE.equals(statutJoueur.get(cle));
                if (ok) atteint++;
                String label = buildLabel(plan.getBaseId(), cle, unitMap, optionsMap);
                details.add(new DetailRow(plan.getBaseId(), label, ok));
            }

            Double pourcentage = lignes.isEmpty() ? null : 100.0 * atteint / lignes.size();
            resultat.put(priorite, new PrioriteSummary(priorite, atteint, lignes.size(), pourcentage, details));
        }

        return new PlayerOmicronProgress(resultat);
    }

    public GlobalSummary calculerGlobalProgression(PlayerOmicronProgress progress) {
        int totalAtteint = 0;
        int totalTotal = 0;

        for (PrioriteSummary ps : progress.parPriorite().values()) {
            totalAtteint += ps.atteint();
            totalTotal += ps.total();
        }

        Double pct = totalTotal > 0 ? (100.0 * totalAtteint / totalTotal) : null;
        return new GlobalSummary("GLOBAL", totalAtteint, totalTotal, pct);
    }

    /** Liste ordonnée des colonnes (un omicron par colonne), toutes priorités confondues. Indépendante du joueur. */
    public List<OmicronColonneDetail> getColonnesDetail(List<OmicronPlan> plans) {
        Map<String, String> unitMap = buildUnitMap();
        Map<String, OmicronPlanService.Option> optionsMap = buildOptionsMap();

        List<OmicronColonneDetail> colonnes = new ArrayList<>();
        for (OmicronPlan plan : plans) {
            String cle = plan.getBaseId() + "|" + plan.getIdSkill();
            String label = buildLabel(plan.getBaseId(), cle, unitMap, optionsMap);
            int priorite = plan.getPriorite() != null ? plan.getPriorite() : 0;
            colonnes.add(new OmicronColonneDetail(cle, plan.getBaseId(), plan.getIdSkill(), label, priorite));
        }

        colonnes.sort(Comparator
                .comparing(OmicronColonneDetail::priorite)
                .thenComparing(OmicronColonneDetail::label, String.CASE_INSENSITIVE_ORDER));

        return colonnes;
    }
}