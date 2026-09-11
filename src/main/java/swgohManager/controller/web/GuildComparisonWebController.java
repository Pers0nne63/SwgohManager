package swgohManager.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import swgohManager.repository.GuildComparisonResultRepository;
import swgohManager.service.ExternalGuildScanService;
import swgohManager.service.GuildComparisonViewService;
import swgohManager.service.SyncProgressService;

@Controller
@RequestMapping("/guilde-comparaison")
@RequiredArgsConstructor
@Slf4j
public class GuildComparisonWebController {

    private final ExternalGuildScanService externalGuildScanService;
    private final GuildComparisonResultRepository guildComparisonResultRepository;
    private final GuildComparisonViewService guildComparisonViewService;
    private final SyncProgressService syncProgressService;

    @GetMapping
    public String formulaire(Model model) {
        model.addAttribute("comparaisons", guildComparisonResultRepository.findAll());
        return "guilde-comparaison-select";
    }

    @PostMapping("/scanner")
    public String scanner(@RequestParam String guildId) {
        externalGuildScanService.lancerComparaisonAsync(guildId.trim());
        return "redirect:/guilde-comparaison?syncing=" + guildId.trim();
    }

    @GetMapping("/progress")
    public SseEmitter progress() {
        return syncProgressService.createEmitter("guildComparison");
    }

    @GetMapping("/{guildId}")
    public String voir(@PathVariable String guildId, Model model) {
        var vm = guildComparisonViewService.construire(guildId);
        if (vm == null) return "redirect:/guilde-comparaison";
        model.addAttribute("vm", vm);
        return "guilde-comparaison";
    }
}