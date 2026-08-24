package swgohManager.controller.web;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.model.PlanFarmInd;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlanFarmIndRepository;
import swgohManager.repository.UnitDefinitionRepository;
import swgohManager.service.FarmPlanIndProgressService;
import swgohManager.service.FarmPlanService;

@Controller
@RequestMapping("/web/plan-farm-ind")
@RequiredArgsConstructor
public class FarmPlanIndWebController {

    private final FarmPlanIndProgressService farmPlanIndProgressService;
    private final FarmPlanService farmPlanService;
    private final JoueurRepository joueurRepository;
    private final PlanFarmIndRepository planFarmIndRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    /**
     * Vue d'ensemble : Liste des membres présents dans la guilde et de leur progression (plan-farm-ind.html)
     */
    @GetMapping
    public String listePlansIndividuels(Model model) {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();
        List<String> playerIds = joueurs.stream()
                .map(Joueur::getPlayerId)
                .toList();

        Map<String, Double> pourcentages = farmPlanIndProgressService.getPourcentagesPourJoueurs(playerIds);

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("pourcentages", pourcentages);

        return "plan-farm-ind";
    }

    /**
     * Vue détaillée : Plan individuel d'un joueur spécifique (plan-farm-ind-detail.html)
     */
    @GetMapping("/joueur/{playerId}")
    public String afficherPlanIndividuel(@PathVariable String playerId, Model model) {
        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("progressionInd", farmPlanIndProgressService.getProgression(playerId));
        model.addAttribute("unites", farmPlanService.getUnitesDisponibles());
        model.addAttribute("etoilesOptions", IntStream.rangeClosed(1, 7).boxed().sorted(Comparator.reverseOrder()).toList());
        model.addAttribute("relicOptions", IntStream.rangeClosed(0, 10).boxed().sorted(Comparator.reverseOrder()).toList());

        return "plan-farm-ind-detail";
    }

    /**
     * Action : Ajouter un objectif au plan individuel
     */
    @PostMapping("/joueur/{playerId}/ajouter")
    public String ajouterObjetIndividuel(
            @PathVariable String playerId,
            @RequestParam String selection, // Contient le libellé sélectionné
            @RequestParam Integer etoiles,
            @RequestParam Integer relic) {

        // 1. Chercher le vrai baseId SWGOH à partir du libellé envoyé
        String baseId = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getLibelle() != null && u.getLibelle().equalsIgnoreCase(selection.trim()))
                .findFirst()
                .map(UnitDefinition::getBaseId)
                .orElse(selection);

        // 2. Créer et sauvegarder le plan individuel avec le bon baseId
        PlanFarmInd plan = new PlanFarmInd();
        plan.setPlayerId(playerId);
        plan.setBaseId(baseId);
        plan.setEtoilesCible(etoiles);
        plan.setRelicCible(relic);
        plan.setDateAjout(Instant.now());

        planFarmIndRepository.save(plan);

        // 3. Mise à jour directe de PlayerPdfIndActuel (sans historisation)
        farmPlanIndProgressService.calculerEtEnregistrer(playerId, null);

        return "redirect:/web/plan-farm-ind/joueur/" + playerId;
    }

    /**
     * Action : Supprimer un objectif du plan individuel
     */
    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        PlanFarmInd plan = planFarmIndRepository.findById(id).orElseThrow();
        String playerId = plan.getPlayerId();

        planFarmIndRepository.deleteById(id);

        // Mise à jour directe de PlayerPdfIndActuel (sans historisation)
        farmPlanIndProgressService.calculerEtEnregistrer(playerId, null);

        return "redirect:/web/plan-farm-ind/joueur/" + playerId;
    }
}