package swgohManager.service;

import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.OmicronPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalPlayerOmicronPlanProgressService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final ExternalRosterUnitSkillActuelRepository externalRosterUnitSkillActuelRepository;
    private final OmicronPlanCalculationService omicronPlanCalculationService;

    public OmicronPlanCalculationService.PlayerOmicronProgress getProgression(String playerId) {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();
        List<PlayerOmicronStatusProjection> statutRows = externalRosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId);
        return omicronPlanCalculationService.calculerProgression(plans, statutRows);
    }

    public OmicronPlanCalculationService.GlobalSummary getGlobalProgression(String playerId) {
        return omicronPlanCalculationService.calculerGlobalProgression(getProgression(playerId));
    }

    public List<OmicronPlanCalculationService.OmicronColonneDetail> getColonnesDetail() {
        return omicronPlanCalculationService.getColonnesDetail(omicronPlanRepository.findAll());
    }

    public Map<String, Boolean> getStatutDetailParJoueur(String playerId) {
        return omicronPlanCalculationService.statutParCle(externalRosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId));
    }
}