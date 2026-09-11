package swgohManager.controller;

import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import swgohManager.service.SyncProgressService;
import swgohManager.service.orchestrator.GacSyncOrchestratorService;
import swgohManager.service.orchestrator.GameDataSyncOrchestratorService;
import swgohManager.service.orchestrator.GuildSyncOrchestratorService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SyncProgressService progressService;
    private final TaskExecutor syncTaskExecutor;

    private final GameDataSyncOrchestratorService gameDataSyncOrchestrator;
    private final GuildSyncOrchestratorService guildSyncOrchestrator;
    private final GacSyncOrchestratorService gacSyncOrchestrator;

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
        syncTaskExecutor.execute(() -> gameDataSyncOrchestrator.runSync(true));
        return ResponseEntity.ok("Synchronisation GameData démarrée");
    }

    @PostMapping("/sync/guild-full")
    @ResponseBody
    public ResponseEntity<String> syncGuildFull() {
        syncTaskExecutor.execute(() -> guildSyncOrchestrator.runFullSync(true));
        return ResponseEntity.ok("Synchronisation Guilde complète démarrée");
    }

    @PostMapping("/sync/gac-full")
    @ResponseBody
    public ResponseEntity<String> syncGacFull() {
        syncTaskExecutor.execute(() -> gacSyncOrchestrator.runFullSync(true));
        return ResponseEntity.ok("Synchronisation GAC démarrée");
    }
}