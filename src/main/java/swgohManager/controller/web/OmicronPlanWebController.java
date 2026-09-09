package swgohManager.controller.web;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import swgohManager.service.OmicronExportService;
import swgohManager.service.OmicronPlanCalculationService;
import swgohManager.service.OmicronPlanProgressService;
import swgohManager.service.OmicronPlanService;
import swgohManager.service.Portee;

@Controller
@RequestMapping("/omicron-tw")
@RequiredArgsConstructor
public class OmicronPlanWebController {

    private final OmicronPlanService omicronPlanService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final OmicronExportService omicronExportService;
    private final JoueurRepository joueurRepository;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("plans", omicronPlanService.getAllEnrichis());
        model.addAttribute("options", omicronPlanService.getOptionsDisponibles().stream()
                .sorted(Comparator.comparing(OmicronPlanService.Option::label))
                .toList());
        model.addAttribute("prioriteOptions", java.util.List.of(1, 2, 3, 4));
        return "omicron-tw";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String selection, @RequestParam Integer priorite) {
        omicronPlanService.ajouter(selection, priorite);
        return "redirect:/omicron-tw";
    }

    @PostMapping("/modifier/{id}")
    public String modifier(@PathVariable Long id, @RequestParam Integer priorite) {
        omicronPlanService.modifier(id, priorite);
        return "redirect:/omicron-tw";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        omicronPlanService.supprimer(id);
        return "redirect:/omicron-tw";
    }
    
    @GetMapping("/commun")
    public String pageCommun(Model model) {
        List<Joueur> joueurs = joueurRepository.findByPresentInGuildTrueOrderByPlayerNameAsc();
        List<String> playerIds = joueurs.stream().map(Joueur::getPlayerId).toList();

        // La map contient maintenant les 4 priorités pour chaque joueur
        Map<String, Map<String, Double>> pourcentages = omicronPlanProgressService.getPourcentagesOmiPourJoueurs(playerIds);

        model.addAttribute("joueurs", joueurs);
        model.addAttribute("pourcentages", pourcentages);

        return "omicron-progress-commun";
    }
    
    @GetMapping("/commun/export")
    @ResponseBody
    public ResponseEntity<byte[]> exporterDetail() throws IOException {
        byte[] fichier = omicronExportService.exportDetailXlsx();

        String filename = "omicrons-detail-" + java.time.LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fichier);
    }
    
    @GetMapping("/api/manquants")
    @ResponseBody
    public List<String> getOmicronsManquants(@RequestParam String playerId, @RequestParam int priorite) {
    	OmicronPlanCalculationService.PlayerOmicronProgress progress = omicronPlanProgressService.getProgression(playerId, Portee.GUILDE);
    	OmicronPlanCalculationService.PrioriteSummary summary = progress.parPriorite().get(priorite);

        if (summary == null) return List.of();

        return summary.details().stream()
                .filter(d -> !d.atteint())
                .map(OmicronPlanCalculationService.DetailRow::label)
                .toList();
    }
}