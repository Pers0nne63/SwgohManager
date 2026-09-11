package swgohManager.service.orchestrator;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.AbilityDefinitionService;
import swgohManager.service.BattleTargetingRuleService;
import swgohManager.service.CategoryDefinitionService;
import swgohManager.service.DatacronTemplateService;
import swgohManager.service.LocalizationService;
import swgohManager.service.MasteryStatService;
import swgohManager.service.SkillDefinitionService;
import swgohManager.service.StatDefinitionService;
import swgohManager.service.StatProgressionService;
import swgohManager.service.SyncProgressService;
import swgohManager.service.UnitDefinitionService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameDataSyncOrchestratorService {

    private final SyncProgressService progressService;
    private final LocalizationService localizationService;
    private final SkillDefinitionService skillDefinitionService;
    private final StatProgressionService statProgressionService;
    private final UnitDefinitionService unitDefinitionService;
    private final MasteryStatService masteryStatService;
    private final StatDefinitionService statDefinitionService;
    private final DatacronTemplateService datacronTemplateService;
    private final CategoryDefinitionService categoryDefinitionService;
    private final BattleTargetingRuleService battleTargetingRuleService;
    private final AbilityDefinitionService abilityDefinitionService;

    public String runSync(boolean withProgress) {
        try {
            if (withProgress) progressService.notifyProgress("gamedata", 10, "Localisations", "Rafraîchissement des localisations...");
            String locResult = localizationService.rafraichir();

            if (withProgress) progressService.notifyProgress("gamedata", 20, "Compétences", "Synchronisation des compétences...");
            skillDefinitionService.synchroniserDefinitions();

            if (withProgress) progressService.notifyProgress("gamedata", 30, "Stat Progression", "Synchronisation de la progression...");
            statProgressionService.synchroniserStatProgression();

            if (withProgress) progressService.notifyProgress("gamedata", 40, "Unités", "Synchronisation des unités...");
            unitDefinitionService.synchroniserUnites();

            if (withProgress) progressService.notifyProgress("gamedata", 50, "Mastery Stats", "Seeding maîtrise...");
            masteryStatService.seedDonnees();

            if (withProgress) progressService.notifyProgress("gamedata", 60, "Stat Definitions", "Seeding définitions...");
            statDefinitionService.seedDonnees();

            if (withProgress) progressService.notifyProgress("gamedata", 70, "Datacrons", "Synchronisation des Datacrons...");
            datacronTemplateService.synchroniserDatacrons();

            if (withProgress) progressService.notifyProgress("gamedata", 80, "Catégories", "Synchronisation des catégories...");
            categoryDefinitionService.synchroniserCategories();

            if (withProgress) progressService.notifyProgress("gamedata", 90, "Règles de ciblage", "Synchronisation des règles de ciblage...");
            battleTargetingRuleService.synchroniserBattleTargetingRules();

            if (withProgress) progressService.notifyProgress("gamedata", 95, "Abilities", "Synchronisation des Capacités...");
            abilityDefinitionService.synchroniserAbilities();

            if (withProgress) progressService.notifyProgress("gamedata", 100, "Terminé", "Toutes les Game Datas ont été synchronisées !");

            return locResult;
        } catch (Exception e) {
            log.error("Erreur sync GameData", e);
            if (withProgress) progressService.notifyError("gamedata", e.getMessage());
            throw e;
        }
    }
}