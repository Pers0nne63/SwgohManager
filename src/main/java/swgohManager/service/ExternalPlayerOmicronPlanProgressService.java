package swgohManager.service;

import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.UnitDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalPlayerOmicronPlanProgressService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final ExternalRosterUnitSkillActuelRepository externalRosterUnitSkillActuelRepository;
    private final OmicronPlanService omicronPlanService;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record DetailRow(String baseId, String label, boolean atteint) {}
    public record PrioriteSummary(int priorite, int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PlayerOmicronProgress(Map<Integer, PrioriteSummary> parPriorite) {}
    public record GlobalSummary(String label, int atteint, int total, Double pourcentage) {}
    public record OmicronColonneDetail(String cle, String baseId, String idSkill, String label, int priorite) {}

    // --- Helpers factorisés ---

    private Map<String, String> buildUnitMap() {
        return unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));
    }

    private Map<String, OmicronPlanService.Option> buildOptionsMap() {
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

    public PlayerOmicronProgress getProgression(String playerId) {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();
        Map<String, String> unitMap = buildUnitMap();
        Map<String, OmicronPlanService.Option> optionsParBaseIdSkill = buildOptionsMap();

        Map<String, Boolean> statutJoueur = new HashMap<>();
        // Changement de repository ici pour taper sur le joueur externe
        for (PlayerOmicronStatusProjection p : externalRosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId)) {
            statutJoueur.put(p.getBaseId() + "|" + p.getIdSkill(), Boolean.TRUE.equals(p.getOmicronApplied()));
        }

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
                String label = buildLabel(plan.getBaseId(), cle, unitMap, optionsParBaseIdSkill);
                details.add(new DetailRow(plan.getBaseId(), label, ok));
            }

            Double pourcentage = lignes.isEmpty() ? null : 100.0 * atteint / lignes.size();
            resultat.put(priorite, new PrioriteSummary(priorite, atteint, lignes.size(), pourcentage, details));
        }

        return new PlayerOmicronProgress(resultat);
    }

    public GlobalSummary getGlobalProgression(String playerId) {
        PlayerOmicronProgress progress = getProgression(playerId);
        int totalAtteint = 0;
        int totalTotal = 0;

        for (PrioriteSummary ps : progress.parPriorite().values()) {
            totalAtteint += ps.atteint();
            totalTotal += ps.total();
        }

        Double pct = totalTotal > 0 ? (100.0 * totalAtteint / totalTotal) : null;
        return new GlobalSummary("GLOBAL", totalAtteint, totalTotal, pct);
    }

    public List<OmicronColonneDetail> getColonnesDetail() {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();
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

    public Map<String, Boolean> getStatutDetailParJoueur(String playerId) {
        Map<String, Boolean> statutJoueur = new HashMap<>();
        for (PlayerOmicronStatusProjection p : externalRosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId)) {
            statutJoueur.put(p.getBaseId() + "|" + p.getIdSkill(), Boolean.TRUE.equals(p.getOmicronApplied()));
        }
        return statutJoueur;
    }
}