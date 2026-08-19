package swgohManager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import swgohManager.service.GuildSyncService;
import swgohManager.service.GuildFullSyncService;
import swgohManager.service.RosterUnitStatObjectifService;

@RestController
@RequestMapping("/api/guild")
@RequiredArgsConstructor
public class GuildController {

    private final GuildSyncService guildSyncService;
    private final GuildFullSyncService guildFullSyncService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;

    @PostMapping("/sync")
    public ResponseEntity<GuildSyncService.GuildSyncResult> syncGuild() {
        return ResponseEntity.ok(guildSyncService.synchroniserGuilde());
    }

    @PostMapping("/sync-full")
    public ResponseEntity<GuildFullSyncResponse> syncGuildFull() {
        // 1. Synchronisation complète de la guilde
        GuildFullSyncService.GuildFullSyncResult fullSyncResult = guildFullSyncService.synchroniserGuildeComplete();

        // 2. Calcul automatique des objectifs de stats
        String statObjectifResult = rosterUnitStatObjectifService.calculerPourTousLesJoueurs();

        return ResponseEntity.ok(new GuildFullSyncResponse(fullSyncResult, statObjectifResult));
    }

    // DTO de réponse combinée pour /sync-full
    public record GuildFullSyncResponse(
        GuildFullSyncService.GuildFullSyncResult fullSyncResult,
        String statObjectifResult
    ) {}
}