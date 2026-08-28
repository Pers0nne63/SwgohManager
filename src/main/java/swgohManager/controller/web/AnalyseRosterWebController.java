package swgohManager.controller.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.OmicronUnitDTO;
import swgohManager.controller.dto.RelicHeaderDTO;
import swgohManager.controller.dto.SkillHeaderDTO;
import swgohManager.controller.dto.UnitHeaderDTO;
import swgohManager.repository.UnitDefinitionRepository;
import swgohManager.service.AnalyseRosterService;

@Controller
@RequiredArgsConstructor
public class AnalyseRosterWebController {

    private final AnalyseRosterService analyseRosterService;
    private final UnitDefinitionRepository unitDefinitionRepository;

    @GetMapping("/analyse-roster")
    public String analyseRoster(
            @RequestParam(required = false, defaultValue = "omicron") String activeTab,
            @RequestParam(required = false) List<String> skillIds,
            @RequestParam(required = false) List<String> relicBaseIds,
            @RequestParam(required = false) List<Integer> targetRelics,
            Model model) {

        List<OmicronUnitDTO> referentiel = analyseRosterService.getReferentielOmicrons();
        model.addAttribute("referentielOmicrons", referentiel);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("toutesLesUnites", unitDefinitionRepository.findDistinctPlayableBaseIdsAvecLibelle());

        // --- TRAITEMENT ONGLET 1 : OMICRON ---
        if ("omicron".equals(activeTab) && skillIds != null) {
            List<String> validSkillIds = skillIds.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .distinct().toList();

            if (!validSkillIds.isEmpty()) {
                Map<String, UnitHeaderDTO> unitHeadersMap = new LinkedHashMap<>();
                for (String skillId : validSkillIds) {
                    for (OmicronUnitDTO unit : referentiel) {
                        for (OmicronUnitDTO.SkillDTO skill : unit.getSkills()) {
                            if (skill.getIdSkill().equals(skillId)) {
                                String rawType = skill.getSkillType() != null ? skill.getSkillType() : "";
                                String typeCapitalized = !rawType.isBlank()
                                        ? rawType.substring(0, 1).toUpperCase() + rawType.substring(1)
                                        : rawType;

                                String skillTypeAvecNumero = (skill.getNumero() != null)
                                        ? typeCapitalized + " " + skill.getNumero()
                                        : typeCapitalized;

                                SkillHeaderDTO skillHeader = new SkillHeaderDTO(skillId, unit.getUnitName(), skillTypeAvecNumero);
                                unitHeadersMap.computeIfAbsent(unit.getUnitName(), k -> new UnitHeaderDTO(k, new ArrayList<>()))
                                        .getSkills().add(skillHeader);
                            }
                        }
                    }
                }
                model.addAttribute("headersRecherches", new ArrayList<>(unitHeadersMap.values()));
                model.addAttribute("skillsRecherches", validSkillIds);
                model.addAttribute("resultats", analyseRosterService.calculerMatrice(validSkillIds));
            }
        }

        // --- TRAITEMENT ONGLET 2 : RELIC ---
        if ("relic".equals(activeTab) && relicBaseIds != null && targetRelics != null) {
            List<RelicHeaderDTO> relicHeaders = new ArrayList<>();
            for (int i = 0; i < relicBaseIds.size(); i++) {
                String baseId = relicBaseIds.get(i);
                Integer target = (i < targetRelics.size()) ? targetRelics.get(i) : null;
                if (baseId != null && !baseId.isBlank() && target != null) {
                    String uName = referentiel.stream()
                            .filter(u -> u.getBaseId().equals(baseId))
                            .map(OmicronUnitDTO::getUnitName)
                            .findFirst().orElse(baseId);
                    relicHeaders.add(new RelicHeaderDTO(baseId, uName, target));
                }
            }

            if (!relicHeaders.isEmpty()) {
            	model.addAttribute("relicHeaders", relicHeaders);
                model.addAttribute("resultatsRelics", analyseRosterService.calculerMatriceRelics(relicBaseIds, targetRelics));
            }
        }

        return "analyse-roster";
    }
}