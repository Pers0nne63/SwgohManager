package swgohManager.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swgohManager.controller.dto.StatConfigDto;
import swgohManager.model.StatDefinition;
import swgohManager.model.UnitDefinition;
import swgohManager.model.UnitStatPriority;
import swgohManager.repository.StatDefinitionRepository;
import swgohManager.repository.UnitDefinitionRepository;
import swgohManager.repository.UnitStatPriorityRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/statq")
@RequiredArgsConstructor
public class StatConfigWebController {

    private final UnitStatPriorityRepository unitStatPriorityRepository;
    private final StatDefinitionRepository statDefinitionRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record UniteOption(String baseId, String libelle) {}

    @GetMapping
    public String page(Model model) {
        List<StatDefinition> stats = statDefinitionRepository.findByIsStatqTrue().stream()
                .sorted(Comparator.comparing(s -> s.getLibellé() != null ? s.getLibellé() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Map des noms réels d'unités (baseId -> libelle)
        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(
                        u -> u.getBaseId().toUpperCase(),
                        UnitDefinition::getLibelle,
                        (v1, v2) -> v1
                ));

        // Transformation en DTO enrichi
        List<StatConfigDto> configs = unitStatPriorityRepository.findAll().stream()
                .map(c -> StatConfigDto.builder()
                        .id(c.getId())
                        .team(c.getTeam())
                        .baseId(c.getBaseId())
                        .nomUnite(unitMap.getOrDefault(c.getBaseId() != null ? c.getBaseId().toUpperCase() : "", c.getBaseId()))
                        .statId1(c.getStatId1())
                        .statId2(c.getStatId2())
                        .statId3(c.getStatId3())
                        .statId4(c.getStatId4())
                        .build())
                .sorted(Comparator.comparing((StatConfigDto c) -> c.getTeam() != null ? c.getTeam() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(c -> c.getNomUnite() != null ? c.getNomUnite() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Liste des unités pour le champ de recherche à l'ajout
        List<UniteOption> unites = unitDefinitionRepository.findDistinctBaseIdsAvecLibelle().stream()
                .filter(p -> p.getLibelle() != null && !p.getLibelle().startsWith("UNIT_"))
                .map(p -> new UniteOption(p.getBaseId(), p.getLibelle()))
                .distinct()
                .sorted((a, b) -> a.libelle().compareToIgnoreCase(b.libelle()))
                .toList();

        model.addAttribute("stats", stats);
        model.addAttribute("configs", configs);
        model.addAttribute("unites", unites);

        return "statq";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String selection,
                          @RequestParam(required = false) String team,
                          @RequestParam(required = false) Integer statId1,
                          @RequestParam(required = false) Integer statId2,
                          @RequestParam(required = false) Integer statId3,
                          @RequestParam(required = false) Integer statId4) {

        String baseId = resoudreBaseId(selection);
        if (baseId == null || baseId.isBlank()) {
            throw new IllegalArgumentException("Unité non reconnue : " + selection);
        }

        UnitStatPriority config = unitStatPriorityRepository.findByBaseId(baseId)
                .orElse(new UnitStatPriority());

        config.setBaseId(baseId);
        config.setTeam(team != null ? team.trim() : null);
        config.setStatId1(statId1);
        config.setStatId2(statId2);
        config.setStatId3(statId3);
        config.setStatId4(statId4);

        unitStatPriorityRepository.save(config);
        return "redirect:/statq";
    }

    private String resoudreBaseId(String selection) {
        if (selection == null || selection.isBlank()) return null;
        String sel = selection.trim();

        // Chercher d'abord par libellé
        for (UnitDefinition u : unitDefinitionRepository.findAll()) {
            if (u.getLibelle() != null && u.getLibelle().equalsIgnoreCase(sel)) {
                return u.getBaseId();
            }
        }
        // Si c'est déjà un Base ID brut
        return sel;
    }

    @PostMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id,
                           @RequestParam(required = false) String team,
                           @RequestParam(required = false) Integer statId1,
                           @RequestParam(required = false) Integer statId2,
                           @RequestParam(required = false) Integer statId3,
                           @RequestParam(required = false) Integer statId4) {

        unitStatPriorityRepository.findById(id).ifPresent(config -> {
            config.setTeam(team != null ? team.trim() : null);
            config.setStatId1(statId1);
            config.setStatId2(statId2);
            config.setStatId3(statId3);
            config.setStatId4(statId4);
            unitStatPriorityRepository.save(config);
        });

        return "redirect:/statq";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        unitStatPriorityRepository.deleteById(id);
        return "redirect:/statq";
    }
}