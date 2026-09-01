package swgohManager.controller.web;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import swgohManager.service.PlanFarmDatacronOptionsService;
import swgohManager.service.PlanFarmDatacronService;

@Controller
@RequestMapping("/web/plan-farm-datacron")
@RequiredArgsConstructor
public class PlanFarmDatacronWebController {

    private final PlanFarmDatacronOptionsService optionsService;
    private final PlanFarmDatacronService planFarmDatacronService;

    public record OngletSet(
            String setId,
            PlanFarmDatacronOptionsService.SetOptions options,
            PlanFarmDatacronService.SetCartes cartes
    ) {}

    @GetMapping
    public String page(Model model) {
        List<PlanFarmDatacronOptionsService.SetOptions> options = optionsService.construire();
        List<PlanFarmDatacronService.SetCartes> cartes = planFarmDatacronService.listerParSet();

        Map<String, PlanFarmDatacronOptionsService.SetOptions> optionsParSet = options.stream()
                .collect(Collectors.toMap(PlanFarmDatacronOptionsService.SetOptions::setId, o -> o));

        Map<String, PlanFarmDatacronService.SetCartes> cartesParSet = cartes.stream()
                .collect(Collectors.toMap(PlanFarmDatacronService.SetCartes::setId, c -> c));

        TreeSet<String> tousLesSets = new TreeSet<>(Comparator.reverseOrder());
        options.forEach(o -> tousLesSets.add(o.setId()));
        cartes.forEach(c -> tousLesSets.add(c.setId()));

        List<OngletSet> onglets = tousLesSets.stream()
                .map(setId -> new OngletSet(
                        setId,
                        optionsParSet.get(setId),
                        cartesParSet.getOrDefault(setId, new PlanFarmDatacronService.SetCartes(setId, List.of()))
                ))
                .toList();

        model.addAttribute("onglets", onglets);
        return "plan-farm-datacron";
    }

    @PostMapping("/creer")
    public String creer(@RequestParam String setId,
                         @RequestParam(required = false) List<String> mecaniques,
                         @RequestParam(required = false) List<String> statsSelectionnees,
                         @RequestParam Map<String, String> allParams) {
        planFarmDatacronService.creer(setId, mecaniques, statsSelectionnees, allParams);
        return "redirect:/web/plan-farm-datacron";
    }

    @PostMapping("/mecanique/{id}/supprimer")
    public String supprimerMecanique(@PathVariable Long id) {
        planFarmDatacronService.supprimerMecanique(id);
        return "redirect:/web/plan-farm-datacron";
    }

    @PostMapping("/stat/{id}/supprimer")
    public String supprimerStat(@PathVariable Long id) {
        planFarmDatacronService.supprimerStat(id);
        return "redirect:/web/plan-farm-datacron";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimerDatacron(@PathVariable Long id) {
        planFarmDatacronService.supprimerDatacron(id);
        return "redirect:/web/plan-farm-datacron";
    }

    @PostMapping("/set/{setId}/supprimer")
    public String supprimerSet(@PathVariable String setId) {
        planFarmDatacronService.supprimerParSet(setId);
        return "redirect:/web/plan-farm-datacron";
    }
}