package swgohManager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import swgohManager.model.PlanFarmInd;
import swgohManager.repository.PlanFarmIndRepository;

@Controller
@RequestMapping("/plan-farm-ind")
@RequiredArgsConstructor
public class PlanFarmIndController {

    private final PlanFarmIndRepository planFarmIndRepository;

    @PostMapping("/joueur/{playerId}/ajouter")
    public String ajouterAuPlanIndividuel(@PathVariable String playerId,
                                          @RequestParam String selection, // Correspond au nom ou baseId
                                          @RequestParam Integer etoiles,
                                          @RequestParam Integer relic,
                                          @RequestParam(required = false) String tag) {
        
        // Logique pour retrouver le baseId exact à partir de 'selection' (comme pour le global)
        String baseId = selection; // À adapter selon ta méthode de recherche d'unité

        PlanFarmInd plan = PlanFarmInd.builder()
                .playerId(playerId)
                .nomUnite(selection)
                .baseId(baseId)
                .etoilesCible(etoiles)
                .relicCible(relic)
                .tag(tag)
                .build();
                
        planFarmIndRepository.save(plan);
        return "redirect:/joueur/" + playerId + "/plan-farm";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimerDuPlanIndividuel(@PathVariable Long id) {
        PlanFarmInd plan = planFarmIndRepository.findById(id).orElseThrow();
        String playerId = plan.getPlayerId();
        planFarmIndRepository.deleteById(id);
        return "redirect:/joueur/" + playerId + "/plan-farm";
    }
}