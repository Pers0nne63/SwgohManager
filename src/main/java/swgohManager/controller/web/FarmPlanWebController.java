package swgohManager.controller.web;

import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.FarmPlanProgressService;
import swgohManager.service.FarmPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/plan-farm")
@RequiredArgsConstructor
public class FarmPlanWebController {

    private final FarmPlanService farmPlanService;
    private final FarmPlanProgressService farmPlanProgressService;
    private final JoueurRepository joueurRepository; // Injection directe du repository

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

    @GetMapping("/commun")
    public String pageCommun(Model model) {
        // 1. Récupérer uniquement les joueurs actifs dans la guilde
        List<Joueur> joueurs = joueurRepository.findByPresentInGuildTrueOrderByPlayerNameAsc();

        // 2. Extraire la liste des playerIds
        List<String> playerIds = joueurs.stream()
                .map(Joueur::getPlayerId)
                .toList();

        // 3. Récupérer la Map (playerId -> pourcentage)
        Map<String, Double> pourcentages = farmPlanProgressService.getPourcentagesPourJoueurs(playerIds);

        // 4. Passer les données au modèle Thymeleaf
        model.addAttribute("joueurs", joueurs);
        model.addAttribute("pourcentages", pourcentages);

        return "plan-farm-commun";
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