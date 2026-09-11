package swgohManager.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.model.FarmPlan;
import swgohManager.model.OmicronPlan;
import swgohManager.model.SkillDefinition;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.FarmPlanRepository;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.SkillDefinitionRepository;
import swgohManager.repository.UnitDefinitionRepository;

/**
 * Charge en une seule fois le référentiel nécessaire à la synchro d'un lot de joueurs
 * (synchro de guilde complète). À appeler UNE FOIS avant de lancer les threads de synchro
 * par joueur, puis à transmettre le résultat (GuildSyncReferentialCache) à chacun d'eux.
 */
@Service
@RequiredArgsConstructor
public class GuildSyncReferentialService {

    private final SkillDefinitionRepository skillDefinitionRepository;
    private final FarmPlanRepository farmPlanRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final OmicronPlanRepository omicronPlanRepository;
    private final OmicronPlanService omicronPlanService;
    private final UnitStatReferentialService unitStatReferentialService;

    public GuildSyncReferentialCache charger() {
        Map<String, SkillDefinition> skillDefinitions = skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));

        List<FarmPlan> farmPlans = farmPlanRepository.findAll();

        Map<String, String> unitLibelleByBaseId = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (a, b) -> a));

        List<OmicronPlan> omicronPlans = omicronPlanRepository.findAll();

        Map<String, OmicronPlanService.Option> omicronOptionsByCle = new HashMap<>();
        for (OmicronPlanService.Option o : omicronPlanService.getOptionsDisponibles()) {
            omicronOptionsByCle.put(o.baseId() + "|" + o.idSkill(), o);
        }

        UnitStatReferentialService.UnitStatReferentiel statReferentiel = unitStatReferentialService.chargerComplet();

        return new GuildSyncReferentialCache(skillDefinitions, farmPlans, unitLibelleByBaseId,
                omicronPlans, omicronOptionsByCle, statReferentiel);
    }
}