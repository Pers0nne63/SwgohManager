package swgohManager.service;

import java.util.List;
import java.util.Map;

import swgohManager.model.FarmPlan;
import swgohManager.model.OmicronPlan;
import swgohManager.model.SkillDefinition;
import swgohManager.service.UnitStatReferentialService.UnitStatReferentiel;

/**
 * Référentiel commun chargé une seule fois par synchro de guilde complète,
 * puis réutilisé par tous les joueurs traités en parallèle — évite de recharger
 * les mêmes données statiques (definitions, plans, stats de référence) 50 fois par synchro.
 */
public record GuildSyncReferentialCache(
        Map<String, SkillDefinition> skillDefinitions,
        List<FarmPlan> farmPlans,
        Map<String, String> unitLibelleByBaseId,
        List<OmicronPlan> omicronPlans,
        Map<String, OmicronPlanService.Option> omicronOptionsByCle,
        UnitStatReferentiel statReferentiel
) {}