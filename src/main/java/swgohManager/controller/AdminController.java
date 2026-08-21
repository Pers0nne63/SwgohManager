package swgohManager.controller;

import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GacRosterSyncService;
import swgohManager.service.GuildFullSyncService;
import swgohManager.service.LeaderboardModMoyService;
import swgohManager.service.LocalizationService;
import swgohManager.service.MasteryStatService;
import swgohManager.service.RosterUnitStatObjectifService;
import swgohManager.service.SkillDefinitionService;
import swgohManager.service.StatDefinitionService;
import swgohManager.service.StatProgressionService;
import swgohManager.service.StatqCalculService;
import swgohManager.service.SyncProgressService;
import swgohManager.service.UnitDefinitionService;
// Service imports...

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SyncProgressService progressService;
    private final LocalizationService localizationService;
    private final SkillDefinitionService skillDefinitionService;
    private final StatProgressionService statProgressionService;
    private final UnitDefinitionService unitDefinitionService;
    private final MasteryStatService masteryStatService;
    private final StatDefinitionService statDefinitionService;
    private final GuildFullSyncService guildFullSyncService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;
    private final StatqCalculService statqCalculService;
    private final GacRosterSyncService gacRosterSyncService;
    private final LeaderboardModMoyService leaderboardModMoyService;
    
    // Inject custom Spring TaskExecutor
    private final TaskExecutor syncTaskExecutor;

    @GetMapping
    public String adminPage() {
        return "admin";
    }

    @GetMapping("/stream/{type}")
    @ResponseBody
    public SseEmitter streamProgress(@PathVariable String type) {
        return progressService.createEmitter(type);
    }

    @PostMapping("/sync/gamedata")
    @ResponseBody
    public ResponseEntity<String> syncGameData() {
        syncTaskExecutor.execute(() -> {
            try {
                progressService.notifyProgress("gamedata", 10, "Localisations", "Rafraîchissement des localisations...");
                localizationService.rafraichir();

                progressService.notifyProgress("gamedata", 30, "Compétences", "Synchronisation des compétences...");
                skillDefinitionService.synchroniserDefinitions();

                progressService.notifyProgress("gamedata", 50, "Stat Progression", "Synchronisation de la progression...");
                statProgressionService.synchroniserStatProgression();

                progressService.notifyProgress("gamedata", 70, "Unités", "Synchronisation des unités...");
                unitDefinitionService.synchroniserUnites();

                progressService.notifyProgress("gamedata", 85, "Mastery Stats", "Seeding maîtrise...");
                masteryStatService.seedDonnees();

                progressService.notifyProgress("gamedata", 95, "Stat Definitions", "Seeding définitions...");
                statDefinitionService.seedDonnees();

                progressService.notifyProgress("gamedata", 100, "Terminé", "Toutes les Game Datas ont été synchronisées !");
            } catch (Exception e) {
                log.error("Erreur sync GameData", e);
                progressService.notifyError("gamedata", e.getMessage());
            }
        });
        return ResponseEntity.ok("Synchronisation GameData démarrée");
    }

    @PostMapping("/sync/guild-full")
    @ResponseBody
    public ResponseEntity<String> syncGuildFull() {
        syncTaskExecutor.execute(() -> {
            try {
                progressService.notifyProgress("guild", 15, "Guilde & Joueurs", "Récupération guilde et joueurs...");
                var fullResult = guildFullSyncService.synchroniserGuildeComplete();

                progressService.notifyProgress("guild", 70, "Objectifs Stats", "Calcul des objectifs...");
                rosterUnitStatObjectifService.calculerPourTousLesJoueurs();

                progressService.notifyProgress("guild", 90, "Calcul STATQ", "Calcul STATQ...");
                statqCalculService.calculerPourTousLesJoueurs();

                progressService.notifyProgress("guild", 100, "Terminé", fullResult.resume());
            } catch (Exception e) {
                log.error("Erreur sync Guilde", e);
                progressService.notifyError("guild", e.getMessage());
            }
        });
        return ResponseEntity.ok("Synchronisation Guilde complète démarrée");
    }

    @PostMapping("/sync/gac-full")
    @ResponseBody
    public ResponseEntity<String> syncGacFull() {
        syncTaskExecutor.execute(() -> {
            try {
                progressService.notifyProgress("gac", 20, "Top Players", "Synchronisation Top GAC...");
                var gacResult = gacRosterSyncService.synchroniserTopPlayers();

                progressService.notifyProgress("gac", 75, "Mods Moyens", "Calcul des moyennes de mods...");
                leaderboardModMoyService.calculerMoyennes();

                progressService.notifyProgress("gac", 100, "Terminé",
                    String.format("GAC terminé : %d synchronisés sur %d.", gacResult.succes(), gacResult.totalJoueurs()));
            } catch (Exception e) {
                log.error("Erreur sync GAC", e);
                progressService.notifyError("gac", e.getMessage());
            }
        });
        return ResponseEntity.ok("Synchronisation GAC démarrée");
    }
}