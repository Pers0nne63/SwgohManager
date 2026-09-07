package swgohManager.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.repository.ExternalPlayerRepository;
import swgohManager.service.ExternalPlayerSyncService;
import swgohManager.service.ExternalPlayerViewService;

@Controller
@RequestMapping("/joueur-externe")
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerWebController {

    private final ExternalPlayerSyncService externalPlayerSyncService;
    private final ExternalPlayerViewService externalPlayerViewService;
    private final ExternalPlayerRepository externalPlayerRepository;

    @GetMapping
    public String formulaire(Model model) {
        model.addAttribute("joueursScannes", externalPlayerRepository.findAllByOrderByPlayerNameAsc());
        return "joueur-externe-select";
    }

    @PostMapping("/scanner")
    public String scanner(@RequestParam String allyCode, Model model) {
        try {
            String playerId = externalPlayerSyncService.scanner(allyCode.trim());
            return "redirect:/joueur-externe/" + playerId;
        } catch (Exception e) {
            log.error("Échec du scan pour l'allycode {} : {}", allyCode, e.getMessage(), e);
            model.addAttribute("erreur", "Impossible de scanner ce joueur. Vérifie l'allycode et réessaie.");
            model.addAttribute("joueursScannes", externalPlayerRepository.findAllByOrderByPlayerNameAsc()); // 👈 ajouté
            return "joueur-externe-select";
        }
    }

    @GetMapping("/{playerId}")
    public String voir(@PathVariable String playerId, Model model) {
        ExternalPlayerViewService.ExternalPlayerViewModel vm = externalPlayerViewService.construire(playerId);
        if (vm.joueur() == null) {
            return "redirect:/joueur-externe";
        }
        model.addAttribute("vm", vm);
        return "joueur-externe";
    }
    
    
}