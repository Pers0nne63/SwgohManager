package swgohManager.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.repository.KyberStatSnapshotRepository;
import swgohManager.service.orchestrator.GacSyncOrchestratorService;

/**
 * Planifie les scans GAC à partir des dates réelles de l'event renvoyées par Comlink :
 *  - un scan juste après le début de la première semaine d'une session ;
 *  - un scan juste après la fin de chaque semaine (donc aussi de la dernière).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GacScanPlanner {

    private static final String NOM_EVENT_GAC = "TERRITORY_TOURNAMENT_EVENT_NAME";

    /** Délai après le début de la semaine avant de scanner. */
    private static final Duration DECALAGE_DEBUT = Duration.ofMinutes(5);

    /**
     * Délai par rapport à la fin de la semaine. Positif = après la fin.
     * Si l'event disparaît de Comlink dès endTime, passer à Duration.ofMinutes(-10).
     */
    private static final Duration DECALAGE_FIN = Duration.ofMinutes(5);

    /** Un scan de début de session n'a lieu que si le dernier snapshot a plus de 3 jours avant la semaine. */
    private static final Duration SEUIL_NOUVELLE_SESSION = Duration.ofDays(3);

    /** On planifie le scan de fin dans l'heure qui précède (le contrôle tourne toutes les heures). */
    private static final Duration FENETRE_PLANIFICATION = Duration.ofMinutes(70);

    /** Si l'appli était arrêtée au moment prévu, on rattrape pendant 2 heures. */
    private static final Duration FENETRE_RATTRAPAGE = Duration.ofHours(2);

    private final SwgohApiClient apiClient;
    private final GacSyncOrchestratorService orchestrator;
    private final KyberStatSnapshotRepository snapshotRepository;
    private final TaskScheduler taskScheduler;

    private final Set<String> dejaTraites = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 5 * * * *", zone = "Europe/Paris")
    public void planifier() {
        try {
            var gac = apiClient.getEvents().gameEvent().stream()
                    .filter(e -> NOM_EVENT_GAC.equals(e.nameKey()))
                    .findFirst()
                    .orElse(null);
            if (gac == null || gac.instance() == null || gac.instance().isEmpty()) {
                return; // pas de GAC en cours (pause entre deux sessions)
            }

            Instant maintenant = Instant.now();

            for (var inst : gac.instance()) {
                Instant debut = Instant.ofEpochMilli(Long.parseLong(inst.startTime()));
                Instant fin = Instant.ofEpochMilli(Long.parseLong(inst.endTime()));

                planifierScanFin(inst.id(), fin, maintenant);
                lancerScanDebutSiNecessaire(inst.id(), debut, fin, maintenant);
            }
        } catch (Exception e) {
            log.error("[GAC] Erreur planification : {}", e.getMessage(), e);
        }
    }

    private void planifierScanFin(String instanceId, Instant fin, Instant maintenant) {
        Instant scanFin = fin.plus(DECALAGE_FIN);
        String cle = instanceId + ":fin";
        String motif = "fin de semaine " + instanceId;

        if (scanFin.isAfter(maintenant)) {
            if (scanFin.isBefore(maintenant.plus(FENETRE_PLANIFICATION)) && dejaTraites.add(cle)) {
                log.info("[GAC] Scan de fin de semaine planifié à {}", scanFin);
                taskScheduler.schedule(() -> scanner(motif), scanFin);
            }
        } else if (maintenant.isBefore(scanFin.plus(FENETRE_RATTRAPAGE)) && dejaTraites.add(cle)) {
            log.info("[GAC] Rattrapage du scan de fin de semaine {}", instanceId);
            scanner(motif);
        }
    }

    private void lancerScanDebutSiNecessaire(String instanceId, Instant debut, Instant fin, Instant maintenant) {
        String cle = instanceId + ":debut";
        Instant scanDebut = debut.plus(DECALAGE_DEBUT);

        boolean semaineEnCours = !maintenant.isBefore(scanDebut) && maintenant.isBefore(fin);
        if (!semaineEnCours || dejaTraites.contains(cle) || !estDebutDeSession(debut)) {
            return;
        }
        // Marqué "traité" seulement en cas de succès : en cas d'échec, nouvel essai à l'heure suivante
        if (scanner("début de session " + instanceId)) {
            dejaTraites.add(cle);
        }
    }

    /** Vrai si le dernier snapshot Kyber date de plus de 3 jours avant le début de cette semaine. */
    private boolean estDebutDeSession(Instant debutSemaine) {
        var dernier = snapshotRepository.findFirstByOrderByDateSnapshotDesc();
        if (dernier == null) {
            return true;
        }
        // dateSnapshot est un LocalDateTime créé avec LocalDateTime.now() : fuseau du serveur
        Instant dateDernier = dernier.getDateSnapshot().atZone(ZoneId.systemDefault()).toInstant();
        return dateDernier.isBefore(debutSemaine.minus(SEUIL_NOUVELLE_SESSION));
    }

    /** @return true si le scan s'est déroulé sans exception */
    private boolean scanner(String motif) {
        log.info("--- [GAC] Début synchronisation GAC Full ({}) ---", motif);
        try {
            var result = orchestrator.runFullSync(false);
            log.info("[GAC] GAC Full terminé : {}", result.modMoyenResult());
            return true;
        } catch (IllegalStateException e) {
            log.warn("[GAC] GAC Full ignorée : {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[GAC] Erreur GAC Full : {}", e.getMessage(), e);
            return false;
        }
    }
}