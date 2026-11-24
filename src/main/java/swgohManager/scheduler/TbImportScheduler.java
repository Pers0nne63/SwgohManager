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
}