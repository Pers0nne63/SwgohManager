package swgohManager.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.service.GuildOverviewService;
import swgohManager.service.TbStatsService;

@Controller
@RequiredArgsConstructor
public class GuildWebController {

    private final GuildOverviewService guildOverviewService;
    private final TbStatsService tbStatsService;
    private final TbScoreJoueurRepository tbScoreJoueurRepository;

    @GetMapping({"/", "/guilde"})
    public String guilde(Model model) {
        var joueurs = guildOverviewService.getJoueurs();

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("raid", guildOverviewService.getDernierRaid());
        model.addAttribute("tb", guildOverviewService.getDerniereTb());
        model.addAttribute("tbSynthese", tbStatsService.getSyntheseTb(null));
        model.addAttribute("tbMsSynthese", tbScoreJoueurRepository.findGuildTbMSStats());

        return "guilde";
    }
}