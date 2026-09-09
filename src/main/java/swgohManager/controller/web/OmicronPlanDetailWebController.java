package swgohManager.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.OmicronPlanProgressService;
import swgohManager.service.Portee;

@Controller
@RequiredArgsConstructor
public class OmicronPlanDetailWebController {

    private final OmicronPlanProgressService omicronPlanProgressService;
    private final JoueurRepository joueurRepository;

    @GetMapping("/joueur/{playerId}/omicron-tw")
    public String detail(@PathVariable String playerId, Model model) {
        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("progression", omicronPlanProgressService.getProgression(playerId, Portee.GUILDE));
        return "omicron-tw-detail";
    }
}