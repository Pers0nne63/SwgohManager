package swgohManager.controller.web;

import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.FarmPlanProgressService;
import swgohManager.service.Portee;

@Controller
@RequiredArgsConstructor
public class FarmPlanDetailWebController {

    private static final String SANS_TAG = "Sans tag";

    private final FarmPlanProgressService farmPlanProgressService;
    private final JoueurRepository joueurRepository;

    @GetMapping("/joueur/{playerId}/plan-farm")
    public String detail(@PathVariable String playerId, Model model) {
        var progression = farmPlanProgressService.getProgression(playerId, Portee.GUILDE);

        // Regroupement par tag, ordre alphabétique, "Sans tag" en dernier
        Comparator<String> ordreTags = Comparator
                .comparing((String t) -> SANS_TAG.equals(t))
                .thenComparing(String.CASE_INSENSITIVE_ORDER);

        var detailsParTag = progression.details().stream().collect(Collectors.groupingBy(
                d -> (d.tag() == null || d.tag().isBlank()) ? SANS_TAG : d.tag().trim(),
                () -> new TreeMap<>(ordreTags),
                Collectors.toList()));

        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("progression", progression);
        model.addAttribute("detailsParTag", detailsParTag);
        return "plan-farm-detail";
    }
}