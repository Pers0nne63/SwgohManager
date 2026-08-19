package swgohManager.controller.web;

import swgohManager.repository.JoueurRepository;
import swgohManager.service.OmicronPlanProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class OmicronPlanDetailWebController {

    private final OmicronPlanProgressService omicronPlanProgressService;
    private final JoueurRepository joueurRepository;

    @GetMapping("/joueur/{playerId}/omicron-tw")
    public String detail(@PathVariable String playerId, Model model) {
        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("progression", omicronPlanProgressService.getProgression(playerId));
        return "omicron-tw-detail";
    }
}