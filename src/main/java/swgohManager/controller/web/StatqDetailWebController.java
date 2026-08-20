package swgohManager.controller.web;

import swgohManager.model.PlayerStatqActuel;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerStatqActuelRepository;
import swgohManager.service.StatqDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class StatqDetailWebController {

    private final StatqDetailService statqDetailService;
    private final JoueurRepository joueurRepository;
    private final PlayerStatqActuelRepository playerStatqActuelRepository;

    public record GuildStatqSummary(double min, double moyenne, double mediane, double max) {}

    @GetMapping("/joueur/{playerId}/statq-detail")
    public String detail(@PathVariable String playerId, Model model) {
        model.addAttribute("joueur", joueurRepository.findByPlayerId(playerId).orElse(null));
        model.addAttribute("detailParTeam", statqDetailService.getDetailParTeam(playerId));

        // 1. Récupération du StatQ du joueur consulté
        PlayerStatqActuel statqJoueur = playerStatqActuelRepository.findByPlayerId(playerId).orElse(null);
        model.addAttribute("statqJoueur", statqJoueur != null ? statqJoueur.getStatq() : null);

        // 2. Calcul des indicateurs de la guilde (Min, Moy, Médiane, Max)
        List<Double> tousLesStatq = playerStatqActuelRepository.findAll().stream()
                .map(PlayerStatqActuel::getStatq)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        model.addAttribute("statsGuilde", calculerStatsGuilde(tousLesStatq));

        return "statq-detail";
    }

    private GuildStatqSummary calculerStatsGuilde(List<Double> valeurs) {
        if (valeurs.isEmpty()) {
            return new GuildStatqSummary(0, 0, 0, 0);
        }

        double min = valeurs.get(0);
        double max = valeurs.get(valeurs.size() - 1);
        double moy = valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double mediane;
        int size = valeurs.size();
        if (size % 2 == 1) {
            mediane = valeurs.get(size / 2);
        } else {
            mediane = (valeurs.get(size / 2 - 1) + valeurs.get(size / 2)) / 2.0;
        }

        return new GuildStatqSummary(min, moy, mediane, max);
    }
}