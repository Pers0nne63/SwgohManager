package swgohManager.service;

import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OmicronPlanProgressService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;
    private final OmicronPlanService omicronPlanService;

    public record DetailRow(String baseId, String label, boolean atteint) {}
    public record PrioriteSummary(int priorite, int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PlayerOmicronProgress(Map<Integer, PrioriteSummary> parPriorite) {}
    
    // NOUVEAU RECORD pour la synthèse globale
    public record GlobalSummary(int atteint, int total, Double pourcentage) {}

    public PlayerOmicronProgress getProgression(String playerId) {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();
        Map<String, OmicronPlanService.Option> optionsParBaseIdSkill = new HashMap<>();
        for (OmicronPlanService.Option o : omicronPlanService.getOptionsDisponibles()) {
            optionsParBaseIdSkill.put(o.baseId() + "|" + o.idSkill(), o);
        }

        Map<String, Boolean> statutJoueur = new HashMap<>();
        for (PlayerOmicronStatusProjection p : rosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId)) {
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

                OmicronPlanService.Option option = optionsParBaseIdSkill.get(cle);
                String label = option != null ? option.label() : plan.getBaseId();
                details.add(new DetailRow(plan.getBaseId(), label, ok));
            }

            Double pourcentage = lignes.isEmpty() ? null : 100.0 * atteint / lignes.size();
            resultat.put(priorite, new PrioriteSummary(priorite, atteint, lignes.size(), pourcentage, details));
        }

        return new PlayerOmicronProgress(resultat);
    }

    public Double getPourcentageP1PourJoueur(String playerId) {
        PrioriteSummary p1 = getProgression(playerId).parPriorite().get(1);
        return p1 != null ? p1.pourcentage() : null;
    }

    // NOUVELLE MÉTHODE
    public GlobalSummary getGlobalProgression(String playerId) {
        PlayerOmicronProgress prog = getProgression(playerId);
        int totalAtteint = prog.parPriorite().values().stream().mapToInt(PrioriteSummary::atteint).sum();
        int total = prog.parPriorite().values().stream().mapToInt(PrioriteSummary::total).sum();
        Double pct = total > 0 ? (100.0 * totalAtteint / total) : null;
        return new GlobalSummary(totalAtteint, total, pct);
    }
}