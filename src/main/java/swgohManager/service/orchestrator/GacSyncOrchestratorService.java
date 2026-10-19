package swgohManager.service.orchestrator;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GacRosterSyncService;
import swgohManager.service.KyberStatService;
import swgohManager.service.LeaderboardModMoyService;
import swgohManager.service.SyncProgressService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GacSyncOrchestratorService {

    private static final int POURCENT_DEBUT_KYBER = 85;
    private static final int POURCENT_MAX_KYBER = 98;
    private static final long NB_JOUEURS_ESTIMES = 50_000L; // sert uniquement à faire avancer la barre

    private final SyncProgressService progressService;
    private final GacRosterSyncService gacRosterSyncService;
    private final LeaderboardModMoyService leaderboardModMoyService;
    private final KyberStatService kyberStatService;

    public GacFullSyncResponse runFullSync(boolean withProgress) {
        try {
            if (withProgress) progressService.notifyProgress("gac", 0, "Top Players", "Synchronisation Top GAC...");
            GacRosterSyncService.GacSyncResult syncResult = gacRosterSyncService.synchroniserTopPlayers(withProgress);

            if (withProgress) progressService.notifyProgress("gac", 70, "Mods Moyens", "Calcul des moyennes de mods...");
            String calculResult = leaderboardModMoyService.calculerMoyennes();

            if (withProgress) progressService.notifyProgress("gac", POURCENT_DEBUT_KYBER, "Stats Kyber", "Statistiques globales Kyber...");
            String kyberResult = lancerStatsKyber(withProgress);

            if (withProgress) progressService.notifyProgress("gac", 100, "Terminé",
                    String.format("GAC terminé : %d synchronisés sur %d. %s",
                            syncResult.succes(), syncResult.totalJoueurs(), kyberResult));

            return new GacFullSyncResponse(syncResult, calculResult, kyberResult);
        } catch (Exception e) {
            log.error("Erreur sync GAC", e);
            if (withProgress) progressService.notifyError("gac", e.getMessage());
            throw e;
        }
    }

    /** Une erreur sur les stats Kyber ne doit pas faire échouer la sync GAC principale. */
    private String lancerStatsKyber(boolean withProgress) {
        try {
            return kyberStatService.genererSnapshotSiNecessaire(scannes -> {
                if (!withProgress) return;
                int pourcent = (int) Math.min(POURCENT_MAX_KYBER,
                        POURCENT_DEBUT_KYBER + scannes * (POURCENT_MAX_KYBER - POURCENT_DEBUT_KYBER) / NB_JOUEURS_ESTIMES);
                progressService.notifyProgress("gac", pourcent, "Stats Kyber",
                        String.format("Kyber : %d joueurs scannés...", scannes));
            });
        } catch (Exception e) {
            log.error("Erreur stats Kyber", e);
            return "Stats Kyber en erreur : " + e.getMessage();
        }
    }

    public record GacFullSyncResponse(
            GacRosterSyncService.GacSyncResult syncResult,
            String modMoyenResult,
            String kyberResult
    ) {}
}