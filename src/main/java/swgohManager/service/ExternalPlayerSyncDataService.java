package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.DatacronDTO;
import swgohManager.model.ExternalPlayerDatacronActuel;
import swgohManager.model.ExternalPlayerDatacronAffixActuel;
import swgohManager.model.ExternalRosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.ExternalPlayerDatacronActuelRepository;
import swgohManager.repository.ExternalPlayerDatacronAffixActuelRepository;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.SkillDefinitionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerSyncDataService {

    private final ExternalRosterUnitSkillActuelRepository externalSkillRepository;
    private final ExternalPlayerDatacronActuelRepository externalDatacronRepository;
    private final ExternalPlayerDatacronAffixActuelRepository externalAffixRepository;
    private final SkillDefinitionRepository skillDefinitionRepository;
    private final UnitSkillCalculationService unitSkillCalculationService;
    private final DatacronSyncCalculationService datacronSyncCalculationService; 


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

        UnitSkillCalculationService.UnitSkillBuildResult buildResult =
                unitSkillCalculationService.construireSkillsDto(roster, definitions);

        List<ExternalRosterUnitSkillActuel> skillsList = buildResult.skills().stream()
                .map(dto -> ExternalRosterUnitSkillActuel.builder()
                        .playerId(playerId)
                        .idUnit(dto.idUnit())
                        .idSkill(dto.idSkill())
                        .tier(dto.tier())
                        .type(dto.type())
                        .numero(dto.numero())
                        .skillZeta(dto.skillZeta())
                        .zetaApplied(dto.zetaApplied())
                        .skillOmicron(dto.skillOmicron())
                        .omicronApplied(dto.omicronApplied())
                        .build())
                .collect(Collectors.toList());

        log.info(">>> AVANT saveAll skills : skillsList.size={}", skillsList.size());
        externalSkillRepository.saveAll(skillsList);
        log.info(">>> APRES saveAll skills OK pour {}", playerId);
    }


    private void sauvegarderDatacrons(String playerId, List<PlayerResponse.DatacronRaw> datacronsBruts) {
        externalDatacronRepository.deleteByPlayerId(playerId);
        externalAffixRepository.deleteByPlayerId(playerId);
        externalDatacronRepository.flush();
        externalAffixRepository.flush();

        List<DatacronDTO> datacronsDto = datacronSyncCalculationService.construireDatacronsDto(datacronsBruts);
        if (datacronsDto.isEmpty()) return;

        List<ExternalPlayerDatacronActuel> datacrons = new ArrayList<>();
        List<ExternalPlayerDatacronAffixActuel> affixes = new ArrayList<>();

        for (DatacronDTO d : datacronsDto) {
            datacrons.add(ExternalPlayerDatacronActuel.builder()
                    .playerId(playerId).idDatacron(d.idDatacron()).setId(d.setId()).templateId(d.templateId())
                    .locked(d.locked()).rerollIndex(d.rerollIndex()).rerollCount(d.rerollCount())
                    .focused(d.focused())
                    .build());

            for (DatacronDTO.AffixDTO a : d.affixes()) {
                affixes.add(ExternalPlayerDatacronAffixActuel.builder()
                        .playerId(playerId).idDatacron(d.idDatacron()).ordre(a.ordre())
                        .tag(a.tag()).targetRule(a.targetRule()).abilityId(a.abilityId())
                        .statType(a.statType()).statValue(a.statValue())
                        .requiredUnitTier(a.requiredUnitTier()).requiredRelicTier(a.requiredRelicTier())
                        .scopeIcon(a.scopeIcon())
                        .build());
            }
        }

        externalDatacronRepository.saveAll(datacrons);
        externalAffixRepository.saveAll(affixes);
        log.info("{} datacron(s) externes enregistrés pour {}", datacrons.size(), playerId);
    }  
    
}