package swgohManager.service.orchestrator;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GacRosterSyncService;
import swgohManager.service.LeaderboardModMoyService;
import swgohManager.service.SyncProgressService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GacSyncOrchestratorService {

    private final SyncProgressService progressService;
    private final GacRosterSyncService gacRosterSyncService;
    private final LeaderboardModMoyService leaderboardModMoyService;

    public GacFullSyncResponse runFullSync(boolean withProgress) {
        try {
            if (withProgress) progressService.notifyProgress("gac", 0, "Top Players", "Synchronisation Top GAC...");
            GacRosterSyncService.GacSyncResult syncResult = gacRosterSyncService.synchroniserTopPlayers(withProgress);

            if (withProgress) progressService.notifyProgress("gac", 80, "Mods Moyens", "Calcul des moyennes de mods...");
            String calculResult = leaderboardModMoyService.calculerMoyennes();

            if (withProgress) progressService.notifyProgress("gac", 100, "Terminé",
                    String.format("GAC terminé : %d synchronisés sur %d.", syncResult.succes(), syncResult.totalJoueurs()));

            return new GacFullSyncResponse(syncResult, calculResult);
        } catch (Exception e) {
            log.error("Erreur sync GAC", e);
            if (withProgress) progressService.notifyError("gac", e.getMessage());
            throw e;
        }
    }

    public record GacFullSyncResponse(
            GacRosterSyncService.GacSyncResult syncResult,
            String modMoyenResult
    ) {}
}