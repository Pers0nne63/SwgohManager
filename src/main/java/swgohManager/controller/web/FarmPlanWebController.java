package swgohManager.controller.web;

import swgohManager.service.FarmPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/plan-farm")
@RequiredArgsConstructor
public class FarmPlanWebController {

    private final FarmPlanService farmPlanService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("plans", farmPlanService.getAllEnrichis());
        model.addAttribute("unites", farmPlanService.getUnitesDisponibles());
        model.addAttribute("etoilesOptions",
                IntStream.rangeClosed(1, 7).boxed().sorted(Comparator.reverseOrder()).toList());
        model.addAttribute("relicOptions",
                IntStream.rangeClosed(0, 10).boxed().sorted(Comparator.reverseOrder()).toList());
        return "plan-farm";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String selection,
                           @RequestParam(defaultValue = "7") Integer etoiles,
                           @RequestParam(defaultValue = "6") Integer relic,
                           @RequestParam(required = false) String tag) {
        farmPlanService.ajouter(selection, etoiles, relic, tag);
        return "redirect:/plan-farm";
    }

    @PostMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id,
                            @RequestParam Integer etoiles,
                            @RequestParam Integer relic,
                            @RequestParam(required = false) String tag) {
        farmPlanService.modifier(id, etoiles, relic, tag);
        return "redirect:/plan-farm";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        farmPlanService.supprimer(id);
        return "redirect:/plan-farm";
    }
}