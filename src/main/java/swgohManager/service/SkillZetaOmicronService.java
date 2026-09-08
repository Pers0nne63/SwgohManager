package swgohManager.service;

import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import swgohManager.repository.SkillDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillZetaOmicronService {

    private final SkillDefinitionRepository skillDefinitionRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;
    private final UnitSkillCalculationService unitSkillCalculationService;

    @Transactional
    public String appliquerZetaOmicron() {
        Map<String, SkillDefinition> definitions = skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));
        List<RosterUnitSkillActuel> skills = rosterUnitSkillActuelRepository.findAll();
        int misAJour = 0;
        int nonTrouves = 0;
        for (RosterUnitSkillActuel skill : skills) {
            SkillDefinition def = definitions.get(skill.getIdSkill());
            if (def == null) {
                nonTrouves++;
                continue;
            }
            UnitSkillCalculationService.ZetaOmicronFlags flags =
                    unitSkillCalculationService.calculerFlagsZetaOmicron(skill.getTier(), def);

            skill.setSkillZeta(flags.skillZeta());
            skill.setSkillOmicron(flags.skillOmicron());
            skill.setZetaApplied(flags.zetaApplied());
            skill.setOmicronApplied(flags.omicronApplied());
            misAJour++;
        }
        rosterUnitSkillActuelRepository.saveAll(skills);
        String resultat = String.format("%d skill(s) mis à jour, %d idSkill non trouvé(s) dans le référentiel",
                misAJour, nonTrouves);
        log.info(resultat);
        return resultat;
    }
}