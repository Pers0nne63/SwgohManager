package swgohManager.controller.web;

import swgohManager.service.OmicronPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;

@Controller
@RequestMapping("/omicron-tw")
@RequiredArgsConstructor
public class OmicronPlanWebController {

    private final OmicronPlanService omicronPlanService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("plans", omicronPlanService.getAllEnrichis());
        model.addAttribute("options", omicronPlanService.getOptionsDisponibles().stream()
                .sorted(Comparator.comparing(OmicronPlanService.Option::label))
                .toList());
        model.addAttribute("prioriteOptions", java.util.List.of(1, 2, 3, 4));
        return "omicron-tw";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String selection, @RequestParam Integer priorite) {
        omicronPlanService.ajouter(selection, priorite);
        return "redirect:/omicron-tw";
    }

    @PostMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, @RequestParam Integer priorite) {
        omicronPlanService.modifier(id, priorite);
        return "redirect:/omicron-tw";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        omicronPlanService.supprimer(id);
        return "redirect:/omicron-tw";
    }
}