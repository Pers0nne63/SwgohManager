package swgohManager.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import swgohManager.model.StatDefinition;
import swgohManager.model.UnitStatPriority;
import swgohManager.repository.StatDefinitionRepository;
import swgohManager.repository.UnitStatPriorityRepository;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/statq")
@RequiredArgsConstructor
public class StatConfigWebController {

    private final UnitStatPriorityRepository unitStatPriorityRepository;
    private final StatDefinitionRepository statDefinitionRepository;

    @GetMapping
    public String page(Model model) {
        List<StatDefinition> stats = statDefinitionRepository.findByIsStatqTrue().stream()
                .sorted(Comparator.comparing(s -> s.getLibellé() != null ? s.getLibellé() : "", String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Trie d'abord par Team (en ignorant la casse), puis par BaseId
        List<UnitStatPriority> configs = unitStatPriorityRepository.findAll().stream()
                .sorted(Comparator.comparing((UnitStatPriority c) -> c.getTeam() != null ? c.getTeam() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UnitStatPriority::getBaseId, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("stats", stats);
        model.addAttribute("configs", configs);

        return "statq";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String baseId,
                          @RequestParam(required = false) String team, // 👈
                          @RequestParam(required = false) Integer statId1,
                          @RequestParam(required = false) Integer statId2,
                          @RequestParam(required = false) Integer statId3,
                          @RequestParam(required = false) Integer statId4) {

        UnitStatPriority config = unitStatPriorityRepository.findByBaseId(baseId.trim())
                .orElse(new UnitStatPriority());

        config.setBaseId(baseId.trim());
        config.setTeam(team != null ? team.trim() : null); // 👈
        config.setStatId1(statId1);
        config.setStatId2(statId2);
        config.setStatId3(statId3);
        config.setStatId4(statId4);

        unitStatPriorityRepository.save(config);
        return "redirect:/statq";
    }

    @PostMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id,
                           @RequestParam(required = false) String team, // 👈
                           @RequestParam(required = false) Integer statId1,
                           @RequestParam(required = false) Integer statId2,
                           @RequestParam(required = false) Integer statId3,
                           @RequestParam(required = false) Integer statId4) {

        unitStatPriorityRepository.findById(id).ifPresent(config -> {
            config.setTeam(team != null ? team.trim() : null); // 👈
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