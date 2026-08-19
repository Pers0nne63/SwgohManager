package swgohManager.controller.web;

import swgohManager.repository.JoueurRepository;
import swgohManager.service.FarmPlanProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class FarmPlanDetailWebController {

    private final FarmPlanProgressService farmPlanProgressService;
    private final JoueurRepository joueurRepository;

    @GetMapping("/joueur/{playerId}/plan-farm")
    public String detail(@PathVariable String playerId, Model model) {
        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("progression", farmPlanProgressService.getProgression(playerId));
        return "plan-farm-detail";
    }
}