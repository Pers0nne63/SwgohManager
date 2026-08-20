package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameDataSyncService {

    private final LocalizationService localizationService;
    private final SkillDefinitionService skillDefinitionService;
    private final StatProgressionService statProgressionService;
    private final UnitDefinitionService unitDefinitionService;
    private final MasteryStatService masteryStatService;
    private final StatDefinitionService statDefinitionService;

    public String synchroniserToutesLesDonnees() {
        String resultatLoc = localizationService.rafraichir();
        String resultat = resultatLoc;

        String resultatSkills = skillDefinitionService.synchroniserDefinitions();
        resultat += " | Skills : " + resultatSkills;

        String resultatStatProgression = statProgressionService.synchroniserStatProgression();
        resultat += " | StatProgression : " + resultatStatProgression;

        String resultatUnites = unitDefinitionService.synchroniserUnites();
        resultat += " | Unités : " + resultatUnites;

        String resultatMastery = masteryStatService.seedDonnees();
        resultat += " | " + resultatMastery;

        String resultatStatDefinition = statDefinitionService.seedDonnees();
        resultat += " | " + resultatStatDefinition;

        log.info(resultat);
        return resultat;
    }
}