package swgohManager.controller.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.TbMSStatsProjection;
import swgohManager.model.Joueur;
import swgohManager.model.RaidHistorique;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.RaidHistoriqueRepository;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.service.ActiviteRosterService;
import swgohManager.service.DatacronProgressService;
import swgohManager.service.OmicronPlanProgressService;
import swgohManager.service.PlayerDatacronViewService;
import swgohManager.service.PlayerViewService;
import swgohManager.service.Portee;
import swgohManager.service.RosterUnitDetailService;
import swgohManager.service.TbStatsService;

@Controller
@RequiredArgsConstructor
public class PlayerWebController {

    private final PlayerViewService playerViewService;
    private final TbStatsService tbStatsService;
    private final TbScoreJoueurRepository tbScoreJoueurRepository; 
    private final RaidHistoriqueRepository raidHistoriqueRepository;
    private final JoueurRepository joueurRepository;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final PlayerDatacronViewService playerDatacronViewService; 
    private final DatacronProgressService datacronProgressService;
    private final ActiviteRosterService activiteRosterService;
    private final RosterUnitActuelRepository rosterUnitActuelRepository; 
    private final RosterUnitDetailService rosterUnitDetailService;


    
    @GetMapping("/joueur/{playerId}")
    public String joueur(@PathVariable String playerId,
            @RequestParam(name = "jours", defaultValue = "30") int jours,
            Model model) {
        model.addAttribute("vm", playerViewService.construire(playerId));
        
        // 👇 Récupération de la liste des unités du roster pour l'onglet Roster
        model.addAttribute("roster", rosterUnitActuelRepository.findRosterCartesJoueur(playerId));

        model.addAttribute("joueurs", joueurRepository.findAllByPresentInGuildTrue().stream()
                .sorted(Comparator.comparing(Joueur::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .toList());

        List<RaidHistorique> raids = raidHistoriqueRepository.findTop10ByPlayerIdOrderByEndTimeAsc(playerId);
        model.addAttribute("raidHistorique", raids);

        model.addAttribute("tbSynthese", tbStatsService.getSyntheseTb(playerId));

        List<TbMSStatsProjection> msStats = tbScoreJoueurRepository.findPlayerTbMSStats(playerId);
        model.addAttribute("tbMsStats", msStats);

        model.addAttribute("progressionOmicron", omicronPlanProgressService.getProgression(playerId, Portee.GUILDE));
        model.addAttribute("globalOmicron", omicronPlanProgressService.getGlobalProgression(playerId, Portee.GUILDE));
        


        Joueur joueurCourant = joueurRepository.findByPlayerId(playerId).orElse(null);
        if (joueurCourant != null) {
            model.addAttribute("datacronProgress",
                    datacronProgressService.construireProgressionJoueur(playerId, joueurCourant.getPlayerName()));
        }

        model.addAttribute("activiteParJour", activiteRosterService.getActiviteJoueur(playerId, jours));
        model.addAttribute("joursActivite", jours);

        return "joueur";
    }
    
    @GetMapping("/joueur")
    public String selection(Model model) {
        model.addAttribute("joueurs", joueurRepository.findAllByPresentInGuildTrue().stream()
                .sorted(Comparator.comparing(Joueur::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        return "joueur-select";
    }
    
    @GetMapping("/joueur/{playerId}/datacrons")
    public String datacrons(@PathVariable String playerId, Model model) {
        Joueur joueur = joueurRepository.findByPlayerId(playerId).orElse(null);
        model.addAttribute("joueur", joueur);
        model.addAttribute("joueurs", joueurRepository.findAllByPresentInGuildTrue().stream()
                .sorted(Comparator.comparing(Joueur::getPlayerName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        model.addAttribute("sets", playerDatacronViewService.construire(playerId));
        return "datacrons";
    }
    
    @GetMapping("/joueur/{playerId}/unite/{idUnit}/detail")
    public String detailUnite(@PathVariable String playerId, @PathVariable String idUnit, Model model) {
        model.addAttribute("detail", rosterUnitDetailService.construire(playerId, idUnit));
        return "fragments/roster-detail-fragments :: unitDetail";
    }
}