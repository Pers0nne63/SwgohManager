package swgohManager.service.orchestrator;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GuildFullSyncService;
import swgohManager.service.RosterUnitStatObjectifService;
import swgohManager.service.StatqCalculService;
import swgohManager.service.SyncProgressService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildSyncOrchestratorService {

    private final SyncProgressService progressService;
    private final GuildFullSyncService guildFullSyncService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;
    private final StatqCalculService statqCalculService;

    public GuildFullSyncResponse runFullSync(boolean withProgress) {
        try {
            if (withProgress) progressService.notifyProgress("guild", 0, "Guilde & Joueurs", "Récupération guilde et joueurs...");
            GuildFullSyncService.GuildFullSyncResult fullSyncResult = guildFullSyncService.synchroniserGuildeComplete(withProgress);

            if (withProgress) progressService.notifyProgress("guild", 60, "Objectifs Stats", "Calcul des objectifs...");
            String statObjResult = rosterUnitStatObjectifService.calculerPourTousLesJoueurs();

            if (withProgress) progressService.notifyProgress("guild", 90, "Calcul STATQ", "Calcul STATQ...");
            String statqResult = statqCalculService.calculerPourTousLesJoueurs();

            if (withProgress) progressService.notifyProgress("guild", 100, "Terminé", fullSyncResult.resume());

            return new GuildFullSyncResponse(fullSyncResult, statObjResult, statqResult);
        } catch (Exception e) {
            log.error("Erreur sync Guilde", e);
            if (withProgress) progressService.notifyError("guild", e.getMessage());
            throw e;
        }
    }

    public record GuildFullSyncResponse(
            GuildFullSyncService.GuildFullSyncResult fullSyncResult,
            String statObjectifResult,
            String statqResult
    ) {}
}