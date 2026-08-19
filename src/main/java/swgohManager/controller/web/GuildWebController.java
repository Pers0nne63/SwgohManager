package swgohManager.controller.web;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.service.GuildOverviewService;
import swgohManager.service.OmicronPlanProgressService;
import swgohManager.service.TbStatsService;

@Controller
@RequiredArgsConstructor
public class GuildWebController {

    private final GuildOverviewService guildOverviewService;
    private final TbStatsService tbStatsService;
    private final TbScoreJoueurRepository tbScoreJoueurRepository;
    private final OmicronPlanProgressService omicronPlanProgressService;

    @GetMapping({"/", "/guilde"})
    public String guilde(Model model) {
        var joueurs = guildOverviewService.getJoueurs();

        Map<String, Double> omicronP1ParJoueur = joueurs.stream()
            .collect(Collectors.toMap(
                j -> j.playerId(), // Remplacé j.getPlayerId() par j.id()
                j -> {
                    Double pct = omicronPlanProgressService.getPourcentageP1PourJoueur(j.playerId());
                    return pct != null ? pct : -1.0;
                }
            ));

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("omicronP1ParJoueur", omicronP1ParJoueur);
        model.addAttribute("raid", guildOverviewService.getDernierRaid());
        model.addAttribute("tb", guildOverviewService.getDerniereTb());
        model.addAttribute("tbSynthese", tbStatsService.getSyntheseTb(null));
        model.addAttribute("tbMsSynthese", tbScoreJoueurRepository.findGuildTbMSStats());

        return "guilde";
    }
}