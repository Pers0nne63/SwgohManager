package swgohManager.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.TbAutoImportService;

@Component
@ConditionalOnProperty(name = "app.guionbot.poll-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TbImportScheduler {

    private final TbAutoImportService tbAutoImportService;

    /**
     * Scan guilde quotidien à 18h02 UTC (2 min après le démarrage à 18h00 UTC).
     * Détecte la TB en cours (rounds 1..6), crée l'entité si besoin et planifie
     * l'import tb-mapstats 2 min avant currentRoundEndTime si disponible.
     */
    @Scheduled(cron = "${app.guionbot.guild-cron:0 2 18 * * *}", zone = "UTC")
    public void syncGuild() {
        try {
            String res = tbAutoImportService.synchroniserGuildEtPlanifierFinRound();
            log.info("[CRON TB Guild] {}", res);
        } catch (Exception e) {
            log.warn("Sync guild guionbot échouée : {}", e.getMessage());
        }
    }

    /**
     * Fallback quotidien à 17h58 UTC (2 min avant la fin d'un round démarré à 18h00 UTC).
     * N'appelle tb-mapstats que si une TB est en cours (round 1..6), qu'un plan est défini
     * et qu'aucun import planifié n'a déjà été exécuté pour ce round.
     */
    @Scheduled(cron = "${app.guionbot.mapstats-cron:0 58 17 * * *}", zone = "UTC")
    public void syncMapStatsFinRound() {
        try {
            String res = tbAutoImportService.synchroniserMapStatsSiActif();
            log.info("[CRON TB MapStats] {}", res);
        } catch (Exception e) {
            log.warn("Sync mapstats guionbot échouée : {}", e.getMessage());
        }
    }

    /**
     * Import tb-events (fenêtre glissante des 50 derniers événements par zone, d'où la fréquence).
     * Cadence (heures UTC), un même tick n'appartient qu'à une seule des 4 expressions :
     *  - 00h-10h et 14h-17h : toutes les heures
     *  - 11h-14h            : toutes les 30 min (11h00 ... 13h30, le tick de 14h00 vient de la ligne horaire)
     *  - 18h-20h            : toutes les 15 min (18h00 ... 19h45)
     *  - 20h-00h            : toutes les 30 min (20h00 ... 23h30)
     * Le service ne contacte guionbot que si la dernière TB connue est dans sa fenêtre de 6 jours.
     */
    @Scheduled(cron = "${app.guionbot.events-cron-journee:0 0 0-10,14-17 * * *}", zone = "UTC")
    @Scheduled(cron = "${app.guionbot.events-cron-midi:0 0,30 11-13 * * *}", zone = "UTC")
    @Scheduled(cron = "${app.guionbot.events-cron-debut-soiree:0 0,15,30,45 18-19 * * *}", zone = "UTC")
    @Scheduled(cron = "${app.guionbot.events-cron-soiree:0 0,30 20-23 * * *}", zone = "UTC")
    public void syncEvents() {
        try {
            String res = tbAutoImportService.synchroniserEventsSiActif();
            log.info("[CRON TB Events] {}", res);
        } catch (Exception e) {
            log.warn("Sync tb-events guionbot échouée : {}", e.getMessage());
        }
    }
}