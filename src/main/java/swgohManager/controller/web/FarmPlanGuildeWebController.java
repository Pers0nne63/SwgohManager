package swgohManager.controller.web;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.FarmPlanIndProgressService;
import swgohManager.service.FarmPlanProgressService;

/**
 * Page unique de suivi des plans de farm : une ligne par joueur
 * avec la progression du plan commun et celle du plan individuel.
 */
@Controller
@RequiredArgsConstructor
public class FarmPlanGuildeWebController {

    private final JoueurRepository joueurRepository;
    private final FarmPlanProgressService farmPlanProgressService;       // plan commun
    private final FarmPlanIndProgressService farmPlanIndProgressService; // plans individuels

    @GetMapping("/plan-farm/suivi")
    public String listePlansFarm(Model model) {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();
        List<String> playerIds = joueurs.stream()
                .map(Joueur::getPlayerId)
                .toList();

        // Plan individuel : méthode existante
        Map<String, Double> pourcentagesInd =
                farmPlanIndProgressService.getPourcentagesPourJoueurs(playerIds);

        // Plan commun : À ADAPTER avec l'appel que ton ancien contrôleur de la page "commun" utilisait
        Map<String, Double> pourcentagesCommun =
                farmPlanProgressService.getPourcentagesPourJoueurs(playerIds);

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("pourcentagesCommun", pourcentagesCommun);
        model.addAttribute("pourcentagesInd", pourcentagesInd);

        return "plan-farm-guilde";
    }
}