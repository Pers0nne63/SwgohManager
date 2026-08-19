package swgohManager.service;

import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.SkillRaw;
import swgohManager.model.SkillDefinition;
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
public class SkillDefinitionService {

    private final SwgohDataClient swgohDataClient;
    private final SkillDefinitionRepository skillDefinitionRepository;

    @Transactional
    public String synchroniserDefinitions() {
        String version = swgohDataClient.getLatestGameVersion();
        List<SkillRaw> skillsBruts = swgohDataClient.streamSkillSegment(version);

        Map<String, SkillDefinition> existantes = skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));

        int nouveaux = 0, misAJour = 0;
        List<SkillDefinition> aSauvegarder = new java.util.ArrayList<>();

        for (SkillRaw skill : skillsBruts) {
            Integer tierZetaRequis = null;
            Integer tierOmicronRequis = null;

            if (skill.tier() != null) {
                for (int i = 0; i < skill.tier().size(); i++) {
                    SkillRaw.TierRaw t = skill.tier().get(i);
                    if (tierZetaRequis == null && Boolean.TRUE.equals(t.isZetaTier())) {
                        tierZetaRequis = i + 1;
                    }
                    if (tierOmicronRequis == null && Boolean.TRUE.equals(t.isOmicronTier())) {
                        tierOmicronRequis = i + 1;
                    }
                }
            }

            boolean skillZeta = tierZetaRequis != null;
            boolean skillOmicron = tierOmicronRequis != null;

            SkillDefinition def = existantes.get(skill.id());
            if (def == null) {
                def = new SkillDefinition();
                def.setIdSkill(skill.id());
                nouveaux++;
            } else {
                misAJour++;
            }

            def.setSkillZeta(skillZeta);
            def.setTierZetaRequis(tierZetaRequis);
            def.setSkillOmicron(skillOmicron);
            def.setTierOmicronRequis(tierOmicronRequis);
            def.setGameVersion(version);

            aSauvegarder.add(def);
        }

        skillDefinitionRepository.saveAll(aSauvegarder);

        String resultat = String.format("Version %s : %d nouvelle(s) définition(s), %d mise(s) à jour",
                version, nouveaux, misAJour);
        log.info(resultat);
        return resultat;
    }
}