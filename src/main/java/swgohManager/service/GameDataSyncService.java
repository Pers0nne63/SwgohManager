package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameDataSyncService {

    private final SkillDefinitionService skillDefinitionService;
    private final StatProgressionService statProgressionService;
    private final UnitDefinitionService unitDefinitionService;
    private final MasteryStatService masteryStatService;
    private final StatDefinitionService statDefinitionService;

    public String synchroniserToutesLesDonnees() {
        String resultatSkills = skillDefinitionService.synchroniserDefinitions();
        String resultatStatProgression = statProgressionService.synchroniserStatProgression();
        String resultatUnites = unitDefinitionService.synchroniserUnites();
       
        String resultat = "Skills : " + resultatSkills + " | StatProgression : " + resultatStatProgression;
        resultat += " | Unités : " + resultatUnites;
        
        String resultatMastery = masteryStatService.seedDonnees();
        resultat += " | " + resultatMastery;
        
        String resultatStatDefinition = statDefinitionService.seedDonnees();
        resultat += " | " + resultatStatDefinition;
        
        log.info(resultat);
        return resultat;
    }
}