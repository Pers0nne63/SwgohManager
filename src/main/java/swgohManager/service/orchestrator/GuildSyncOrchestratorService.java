package swgohManager.service.orchestrator;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GuildFullSyncService;
import swgohManager.service.SyncProgressService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildSyncOrchestratorService {

    private final SyncProgressService progressService;
    private final GuildFullSyncService guildFullSyncService;

    public GuildFullSyncResponse runFullSync(boolean withProgress) {
        try {
            if (withProgress) progressService.notifyProgress("guild", 0, "Démarrage", "Récupération guilde et joueurs...");
            
            // La méthode synchroniserGuildeComplete se charge désormais des Objectifs et du STATQ
            GuildFullSyncService.GuildFullSyncResult fullSyncResult = guildFullSyncService.synchroniserGuildeComplete(withProgress);

            if (withProgress) progressService.notifyProgress("guild", 100, "Terminé", fullSyncResult.resume());

            return new GuildFullSyncResponse(fullSyncResult);
        } catch (Exception e) {
            log.error("Erreur sync Guilde", e);
            if (withProgress) progressService.notifyError("guild", e.getMessage());
            throw e;
        }
    }

    public record GuildFullSyncResponse(
            GuildFullSyncService.GuildFullSyncResult fullSyncResult
    ) {}
}