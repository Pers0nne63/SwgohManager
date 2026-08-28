package swgohManager.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.GacRosterSyncService;
import swgohManager.service.GameDataSyncService;
import swgohManager.service.GuildFullSyncService;
import swgohManager.service.LeaderboardModMoyService;
import swgohManager.service.RosterUnitService;
import swgohManager.service.RosterUnitStatObjectifService;
import swgohManager.service.StatqCalculService;

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
    private final RosterUnitService rosterUnitService;

    /**
     * 1. Synchronisation GameData
     * Tous les jeudis à 09h00
     */
    @Scheduled(cron = "0 0 1 ? * THU")
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
    @Scheduled(cron = "0 0 2 ? * WED")
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
    @Scheduled(cron = "0 0 3 * * ?")
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
    
    /**
     * 4. Historisation du Roster
     * Tous les dimanches à 23h00
     */
    @Scheduled(cron = "0 0 4 ? * MON")
    public void historiserRosterHebdomadaire() {
        log.info("--- [CRON] Début de l'historisation du roster (Dimanche 23h00) ---");
        try {
            int nbUnites = rosterUnitService.historiserRosterActuel();
            log.info("[CRON] Historisation terminée avec succès ({} unités).", nbUnites);
        } catch (Exception e) {
            log.error("[CRON] Erreur lors de l'historisation du roster : {}", e.getMessage(), e);
        }
    }
}