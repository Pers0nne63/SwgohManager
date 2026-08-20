package swgohManager.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swgohManager.service.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final GameDataSyncService gameDataSyncService;
    private final GacRosterSyncService gacRosterSyncService;
    private final LeaderboardModMoyService leaderboardModMoyService;
    private final GuildFullSyncService guildFullSyncService;
    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;
    private final StatqCalculService statqCalculService;

    /**
     * 1. Synchronisation GameData
     * Tous les jeudis à 09h00
     */
    @Scheduled(cron = "0 0 9 ? * THU")
    public void syncGameData() {
        log.info("--- [CRON] Début synchronisation GameData (Jeudi 09h00) ---");
        try {
            String result = gameDataSyncService.synchroniserToutesLesDonnees();
            log.info("[CRON] GameData terminé : {}", result);
        } catch (Exception e) {
            log.error("[CRON] Erreur lors de la synchro GameData : {}", e.getMessage(), e);
        }
    }

    /**
     * 2. Synchronisation GAC Full (Top Players + Calcul Mods Moyen)
     * Tous les mercredis à 09h00
     */
    @Scheduled(cron = "0 0 9 ? * WED")
    public void syncGacFull() {
        log.info("--- [CRON] Début synchronisation GAC Full (Mercredi 09h00) ---");
        try {
            // Étape 1 : Synchronisation du top
            var syncResult = gacRosterSyncService.synchroniserTopPlayers();
            log.info("[CRON] GAC Top Players terminé : {}", syncResult);

            // Étape 2 : Calcul des mods moyens
            String calculResult = leaderboardModMoyService.calculerMoyennes();
            log.info("[CRON] Calcul des mods moyens terminé : {}", calculResult);

        } catch (Exception e) {
            log.error("[CRON] Erreur lors de la synchro GAC Full : {}", e.getMessage(), e);
        }
    }

    /**
     * 3. Synchronisation Guilde Full (Guilde + Objectifs + StatQ)
     * Tous les jours à 10h00
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void syncGuildFull() {
        log.info("--- [CRON] Début synchronisation Guilde Full (Quotidien 10h00) ---");
        try {
            // Étape 1 : Synchro complète guilde
            var fullSyncResult = guildFullSyncService.synchroniserGuildeComplete();
            log.info("[CRON] Synchro Guilde terminée : {}", fullSyncResult);

            // Étape 2 : Calcul des objectifs
            String statObjResult = rosterUnitStatObjectifService.calculerPourTousLesJoueurs();
            log.info("[CRON] Calcul Objectifs terminé : {}", statObjResult);

            // Étape 3 : Calcul StatQ
            String statqResult = statqCalculService.calculerPourTousLesJoueurs();
            log.info("[CRON] Calcul StatQ terminé : {}", statqResult);

        } catch (Exception e) {
            log.error("[CRON] Erreur lors de la synchro Guilde Full : {}", e.getMessage(), e);
        }
    }
}