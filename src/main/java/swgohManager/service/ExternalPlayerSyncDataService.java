package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.util.SkillIdParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerSyncDataService {

    private final ExternalRosterUnitSkillActuelRepository externalSkillRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;
    private final SkillDefinitionRepository skillDefinitionRepository;

    @Transactional
    public void enregistrerSkillsEtDatacrons(String playerId, PlayerResponse response) {
        sauvegarderSkills(playerId, response.rosterUnit());
        sauvegarderDatacrons(playerId, response.datacron());
    }

    private void sauvegarderSkills(String playerId, List<PlayerResponse.RosterUnit> roster) {
        log.info(">>> ENTREE sauvegarderSkills pour {} — roster.size={}", playerId, roster != null ? roster.size() : "NULL");

        externalSkillRepository.deleteByPlayerId(playerId);
        externalSkillRepository.flush();

        if (roster == null || roster.isEmpty()) return;

        Map<String, SkillDefinition> definitions = skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));

        List<ExternalRosterUnitSkillActuel> skillsList = new ArrayList<>();

        for (PlayerResponse.RosterUnit u : roster) {
            if (u.skill() == null) continue;

            for (PlayerResponse.Skill s : u.skill()) {
                SkillIdParser.ParsedSkillId parsed = SkillIdParser.parse(s.id());
                SkillDefinition def = definitions.get(s.id());

                Boolean skillZeta = null, skillOmicron = null, zetaApplied = null, omicronApplied = null;

                if (def != null) {
                    skillZeta = def.getSkillZeta();
                    skillOmicron = def.getSkillOmicron();

                    zetaApplied = Boolean.TRUE.equals(skillZeta)
                            && def.getTierZetaRequis() != null
                            && s.tier() != null
                            && s.tier() >= def.getTierZetaRequis();

                    omicronApplied = Boolean.TRUE.equals(skillOmicron)
                            && def.getTierOmicronRequis() != null
                            && s.tier() != null
                            && s.tier() >= (def.getTierOmicronRequis() - 1);
                }

                skillsList.add(ExternalRosterUnitSkillActuel.builder()
                        .playerId(playerId)
                        .idUnit(u.id())
                        .idSkill(s.id())
                        .tier(s.tier())
                        .type(parsed.type())
                        .numero(parsed.numero())
                        .skillZeta(skillZeta)
                        .zetaApplied(zetaApplied)
                        .skillOmicron(skillOmicron)
                        .omicronApplied(omicronApplied)
                        .build());
            }
        }

        log.info(">>> AVANT saveAll skills : skillsList.size={}", skillsList.size());
        externalSkillRepository.saveAll(skillsList);
        log.info(">>> APRES saveAll skills OK pour {}", playerId);
    }

    private void sauvegarderDatacrons(String playerId, List<PlayerResponse.DatacronRaw> datacronsBruts) {
        externalDatacronRepository.deleteByPlayerId(playerId);
        externalAffixRepository.deleteByPlayerId(playerId);
        externalDatacronRepository.flush();
        externalAffixRepository.flush();

        if (datacronsBruts == null || datacronsBruts.isEmpty()) return;

        List<ExternalPlayerDatacronActuel> datacrons = new ArrayList<>();
        List<ExternalPlayerDatacronAffixActuel> affixes = new ArrayList<>();

        for (PlayerResponse.DatacronRaw d : datacronsBruts) {
            datacrons.add(ExternalPlayerDatacronActuel.builder()
                    .playerId(playerId).idDatacron(d.id()).setId(d.setId()).templateId(d.templateId())
                    .locked(d.locked()).rerollIndex(d.rerollIndex()).rerollCount(d.rerollCount())
                    .focused(d.focused())
                    .build());

            if (d.affix() != null) {
                int ordre = 1;
                for (PlayerResponse.AffixRaw a : d.affix()) {
                    affixes.add(ExternalPlayerDatacronAffixActuel.builder()
                            .playerId(playerId).idDatacron(d.id()).ordre(ordre)
                            .tag(a.tag() != null ? String.join(",", a.tag()) : null)
                            .targetRule(a.targetRule()).abilityId(a.abilityId())
                            .statType(a.statType()).statValue(parseLong(a.statValue()))
                            .requiredUnitTier(a.requiredUnitTier()).requiredRelicTier(a.requiredRelicTier())
                            .scopeIcon(a.scopeIcon())
                            .build());
                    ordre++;
                }
            }
        }
        
        log.info(">>> AVANT saveAll datacrons : datacrons.size={}", datacrons.size());
        externalDatacronRepository.saveAll(datacrons);
        externalAffixRepository.saveAll(affixes);
        log.info(">>> APRES saveAll datacrons OK pour {}", playerId);
        log.info("{} datacron(s) externes enregistrés pour {}", datacrons.size(), playerId);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }
    
    
    
}