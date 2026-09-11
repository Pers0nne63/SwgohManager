package swgohManager.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.PlayerSyncService;
import swgohManager.service.RosterUnitService;
import swgohManager.service.orchestrator.GacSyncOrchestratorService;
import swgohManager.service.orchestrator.GameDataSyncOrchestratorService;
import swgohManager.service.orchestrator.GuildSyncOrchestratorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final GameDataSyncOrchestratorService gameDataSyncOrchestrator;
    private final GacSyncOrchestratorService gacSyncOrchestrator;
    private final GuildSyncOrchestratorService guildSyncOrchestrator;
    private final RosterUnitService rosterUnitService;
    private final PlayerSyncService playerSyncService;

    @Scheduled(cron = "0 0 1 ? * THU")
    public void syncGameData() {
        log.info("--- [CRON] Début synchronisation GameData ---");
        try {
            gameDataSyncOrchestrator.runSync(false);
            log.info("[CRON] GameData terminé");
        } catch (Exception e) {
            log.error("[CRON] Erreur GameData : {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 ? * WED")
    public void syncGacFull() {
        log.info("--- [CRON] Début synchronisation GAC Full ---");
        try {
            var result = gacSyncOrchestrator.runFullSync(false);
            log.info("[CRON] GAC Full terminé : {}", result.modMoyenResult());
        } catch (Exception e) {
            log.error("[CRON] Erreur GAC Full : {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void syncGuildFull() {
        log.info("--- [CRON] Début synchronisation Guilde Full ---");
        try {
            var result = guildSyncOrchestrator.runFullSync(false);
            log.info("[CRON] Guilde Full terminée : {}", result.fullSyncResult().resume());
        } catch (Exception e) {
            log.error("[CRON] Erreur Guilde Full : {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 4 ? * MON")
    public void historiserRosterHebdomadaire() {
        log.info("--- [CRON] Début historisation roster ---");
        try {
            int nbUnites = rosterUnitService.historiserRosterActuel();
            log.info("[CRON] Historisation terminée : {} unités", nbUnites);
        } catch (Exception e) {
            log.error("[CRON] Erreur historisation : {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void purgerScansExternes() {
        log.info("--- [CRON] Début purge scans externes ---");
        try {
            int nb = playerSyncService.purgerAnciensScan();
            log.info("[CRON] Purge terminée : {} joueur(s) supprimé(s)", nb);
        } catch (Exception e) {
            log.error("[CRON] Erreur purge : {}", e.getMessage(), e);
        }
    }
}