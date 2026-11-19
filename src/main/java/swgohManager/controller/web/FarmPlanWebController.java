package swgohManager.controller.web;

import swgohManager.service.FarmPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/plan-farm")
@RequiredArgsConstructor
public class FarmPlanWebController {

    private static final String SANS_TAG = "Sans tag";

    private final FarmPlanService farmPlanService;

    @GetMapping
    public String page(Model model) {
        var plans = farmPlanService.getAllEnrichis();

        // Regroupement par tag, ordre alphabétique, "Sans tag" en dernier
        Comparator<String> ordreTags = Comparator
                .comparing((String t) -> SANS_TAG.equals(t))
                .thenComparing(String.CASE_INSENSITIVE_ORDER);

        var plansParTag = plans.stream().collect(Collectors.groupingBy(
                p -> (p.getTag() == null || p.getTag().isBlank()) ? SANS_TAG : p.getTag().trim(),
                () -> new TreeMap<>(ordreTags),
                Collectors.toList()));

        model.addAttribute("plans", plans);
        model.addAttribute("plansParTag", plansParTag);
        // Tags réels pour l'autocomplétion (sans le libellé "Sans tag")
        model.addAttribute("tags", plansParTag.keySet().stream()
                .filter(t -> !SANS_TAG.equals(t))
                .toList());

        model.addAttribute("unites", farmPlanService.getUnitesDisponibles());
        model.addAttribute("etoilesOptions",
                IntStream.rangeClosed(1, 7).boxed().sorted(Comparator.reverseOrder()).toList());
        model.addAttribute("relicOptions",
                IntStream.rangeClosed(0, 10).boxed().sorted(Comparator.reverseOrder()).toList());
        return "plan-farm";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String selection,
                          @RequestParam Integer etoiles,
                          @RequestParam Integer relic,
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