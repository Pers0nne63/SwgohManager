package swgohManager.controller.web;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.OmicronPlanProgressService;
import swgohManager.service.OmicronPlanService;

@Controller
@RequestMapping("/omicron-tw")
@RequiredArgsConstructor
public class OmicronPlanWebController {

    private final OmicronPlanService omicronPlanService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final JoueurRepository joueurRepository;

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
    
    @GetMapping("/commun")
    public String pageCommun(Model model) {
        List<Joueur> joueurs = joueurRepository.findByPresentInGuildTrueOrderByPlayerNameAsc();
        List<String> playerIds = joueurs.stream().map(Joueur::getPlayerId).toList();

        // La map contient maintenant les 4 priorités pour chaque joueur
        Map<String, Map<String, Double>> pourcentages = omicronPlanProgressService.getPourcentagesOmiPourJoueurs(playerIds);

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("pourcentages", pourcentages);

        return "omicron-progress-commun";
    }
}