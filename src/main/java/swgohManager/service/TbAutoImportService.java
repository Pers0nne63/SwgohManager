package swgohManager.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.GuionbotClient;
import swgohManager.controller.dto.TbImportMapStat;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TerritoryBattleRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TbAutoImportService {

    private static final String TB_EVENT_TYPE = "TERRITORYBATTLEEVENT";
    private static final int MAX_ROUNDS = 6;
    private static final long ANTICIPATION_FIN_ROUND_SEC = 120L;
    private static final LocalDate DATE_REFERENCE_TB = LocalDate.of(2026, 11, 23);

    private final GuionbotClient guionbotClient;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbImportStatService tbImportStatService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;
    private final TbEventImportService tbEventImportService;

    private final AtomicReference<ScheduledFuture<?>> tacheFinRoundPlanifiee = new AtomicReference<>();
    private volatile String dernierRoundPlanifieCle = "";
    private volatile String dernierRoundImporteCle = "";

    @Value("${swgoh.guild.id}")
    private String guildId;
    
    private boolean estDansFenetreActive(Instant debutTbOuNull) {
        Instant maintenant = Instant.now();

        // 1) Si on connaît le début exact de la BT en cours : elle dure 6 jours (144h)
        if (debutTbOuNull != null) {
            long heuresEcoulees = Duration.between(debutTbOuNull, maintenant).toHours();
            return heuresEcoulees >= 0 && heuresEcoulees < (6 * 24);
        }

        // 2) Sinon (avant création / sans startTime) : vérifier si on est dans les 6 premiers jours
        //    d'une semaine "BT" (cycle de 14 jours depuis le 23/11/2026 en UTC)
        LocalDate aujourdHuiUtc = maintenant.atZone(ZoneOffset.UTC).toLocalDate();
        long joursDepuisRef = ChronoUnit.DAYS.between(DATE_REFERENCE_TB, aujourdHuiUtc);
        long jourDansCycle14j = Math.floorMod(joursDepuisRef, 14);

        // Jours 0 à 5 du cycle = lundi à samedi (6 jours de la semaine active) ;
        // jours 6 à 13 = dimanche + toute la semaine "off" (1 semaine sur 2)
        return jourDansCycle14j >= 0 && jourDansCycle14j < 6;
    }

    /**
     * Appelé tous les soirs à 18h02 UTC :
     * - lit la guilde pour vérifier si une TB est active (round 1..6),
     * - crée la TerritoryBattle si absente,
     * - si currentRoundEndTime est présent, planifie l'appel tb-mapstats à (endTime - 2 min).
     */
    public String synchroniserGuildEtPlanifierFinRound() {
    	
    	if (!estDansFenetreActive(null)) {
            annulerTachePlanifiee();
            return "Hors fenêtre active de TB (cycle 14j / 6j) : scan guild ignoré";
        }
    	
    	JsonNode guild = guionbotClient.fetchGuild();
        JsonNode tbStatus = guild.path("territoryBattleStatus").path(0);

        String instanceId = tbStatus.path("instanceId").asText("");
        if (instanceId.isBlank()) {
            annulerTachePlanifiee();
            return "Aucune TB en cours (scan guild)";
        }

        int roundNum = tbStatus.path("currentRound").asInt(0);
        if (roundNum <= 0 || roundNum > MAX_ROUNDS) {
            annulerTachePlanifiee();
            return "TB " + instanceId + " : round " + roundNum + " hors plage (1.." + MAX_ROUNDS + ")";
        }

        TerritoryBattle tb = territoryBattleRepository.findByInstanceId(instanceId)
                .orElseGet(() -> creerTerritoryBattle(guild, tbStatus, instanceId));

        if (tb.getPlanId() == null) {
            log.warn("TB {} (round {}) sans plan associé : import de fin de round en attente", instanceId, roundNum);
            return "TB " + instanceId + " (round " + roundNum + ") : aucun plan associé";
        }

        long finRoundMs = tbStatus.path("currentRoundEndTime").asLong(0);
        if (finRoundMs > 0) {
            Instant instantImport = Instant.ofEpochMilli(finRoundMs).minusSeconds(ANTICIPATION_FIN_ROUND_SEC);
            String cleRound = instanceId + "#" + roundNum + "#" + finRoundMs;

            if (instantImport.isAfter(Instant.now()) && !cleRound.equals(dernierRoundPlanifieCle)) {
                annulerTachePlanifiee();
                ScheduledFuture<?> future = taskScheduler.schedule(
                        () -> executerImportFinRoundPlanifie(instanceId, roundNum, cleRound),
                        instantImport
                );
                tacheFinRoundPlanifiee.set(future);
                dernierRoundPlanifieCle = cleRound;
                return "TB " + instanceId + " (round " + roundNum + ") : import mapstats planifié à " + instantImport;
            }
        }

        return "TB " + instanceId + " (round " + roundNum + ") : suivie via cron fin de round";
    }

    /**
     * Appelé au cron de fin de round (17h58 UTC par défaut) :
     * exécute un import tb-mapstats uniquement si une TB est en cours (1..6),
     * a un plan associé et n'a pas déjà été importée pour ce round.
     */
    public String synchroniserMapStatsSiActif() {
    	//On vérifie si on est dans une TB sans se connecter.
    	if (!estDansFenetreActive(null)) {
            annulerTachePlanifiee();
            return "Hors fenêtre active de TB (cycle 14j / 6j) : scan guild ignoré";
        }
    	
    	JsonNode guild = guionbotClient.fetchGuild();
        JsonNode tbStatus = guild.path("territoryBattleStatus").path(0);

        String instanceId = tbStatus.path("instanceId").asText("");
        if (instanceId.isBlank()) {
            return "Aucune TB en cours (cron mapstats ignoré)";
        }

        int roundNum = tbStatus.path("currentRound").asInt(0);
        if (roundNum <= 0 || roundNum > MAX_ROUNDS) {
            return "TB " + instanceId + " : round " + roundNum + " inactif (cron mapstats ignoré)";
        }

        long finRoundMs = tbStatus.path("currentRoundEndTime").asLong(0);
        String cleRound = instanceId + "#" + roundNum + "#" + finRoundMs;
        if (cleRound.equals(dernierRoundImporteCle)) {
            return "TB " + instanceId + " (round " + roundNum + ") : déjà importé pour ce round";
        }

        TerritoryBattle tb = territoryBattleRepository.findByInstanceId(instanceId)
                .orElseGet(() -> creerTerritoryBattle(guild, tbStatus, instanceId));
        
        if (!estDansFenetreActive(tb.getStartTime())) {
            return "TB " + instanceId + " : fenêtre de 6 jours dépassée, import ignoré";
        }

        if (tb.getPlanId() == null) {
            return "TB " + instanceId + " (round " + roundNum + ") : aucun plan associé, import ignoré";
        }

        String resultat = importerMapStatsPourTb(tb, roundNum);
        dernierRoundImporteCle = cleRound;
        return resultat;
    }

    /**
     * Déclenchement manuel (endpoint /api/tb/import/auto).
     */
    public String synchroniser() {
        JsonNode guild = guionbotClient.fetchGuild();

        JsonNode tbStatus = guild.path("territoryBattleStatus").path(0);
        String instanceId = tbStatus.path("instanceId").asText("");
        if (instanceId.isBlank()) {
            return "Aucune TB en cours";
        }

        int roundNum = tbStatus.path("currentRound").asInt(0);
        if (roundNum <= 0 || roundNum > MAX_ROUNDS) {
            return "TB " + instanceId + " : round courant invalide ou hors plage (" + roundNum + ")";
        }

        long finRoundMs = tbStatus.path("currentRoundEndTime").asLong(0);
        if (finRoundMs > 0 && Math.abs(Duration.between(Instant.now(), Instant.ofEpochMilli(finRoundMs)).toSeconds()) < 60) {
            return "Changement de round imminent, import reporté au prochain passage";
        }

        TerritoryBattle tb = territoryBattleRepository.findByInstanceId(instanceId)
                .orElseGet(() -> creerTerritoryBattle(guild, tbStatus, instanceId));

        if (tb.getPlanId() == null) {
            log.warn("TB {} sans plan associé : import des stats en attente", instanceId);
            return "TB " + instanceId + " (round " + roundNum + ") : aucun plan associé, import en attente";
        }

        return importerMapStatsPourTb(tb, roundNum);
    }
    
    /** Import tb-events : ne contacte guionbot que si la dernière TB connue est dans sa fenêtre de 6 jours. */
    public String synchroniserEventsSiActif() {
        TerritoryBattle tb = territoryBattleRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(t -> t.getStartTime() != null)
                .findFirst()
                .orElse(null);
        if (tb == null || !estDansFenetreActive(tb.getStartTime())) {
            return "Aucune TB en cours (events ignorés)";
        }
        TbEventImportService.Resultat r = tbEventImportService.importerDepuisApi();
        return "TB " + tb.getInstanceId() + " : events " + r.lus() + " lus, " + r.nouveaux()
                + " nouveaux, " + r.dejaPresents() + " déjà en base, " + r.ignores() + " ignorés";
    }

    private void executerImportFinRoundPlanifie(String instanceId, int roundNum, String cleRound) {
        if (cleRound.equals(dernierRoundImporteCle)) {
            return;
        }
        try {
            TerritoryBattle tb = territoryBattleRepository.findByInstanceId(instanceId).orElse(null);
            if (tb == null || tb.getPlanId() == null || !estDansFenetreActive(tb.getStartTime())) {
                log.warn("Import planifié ignoré pour TB {} (absence, sans plan ou hors fenêtre 6j)", instanceId);
                return;
            }
            String res = importerMapStatsPourTb(tb, roundNum);
            dernierRoundImporteCle = cleRound;
            log.info("[Planifié fin de round] {}", res);
        } catch (Exception e) {
            log.warn("Erreur lors de l'import planifié de fin de round pour TB {} (round {}) : {}",
                    instanceId, roundNum, e.getMessage());
        }
    }

    private String importerMapStatsPourTb(TerritoryBattle tb, int roundNum) {
        JsonNode data = guionbotClient.fetchMapStats();
        if (!data.isArray()) {
            throw new IllegalStateException("Format inattendu pour tb-mapstats (tableau attendu)");
        }
        List<TbImportMapStat> stats = objectMapper.convertValue(data, new TypeReference<List<TbImportMapStat>>() {});

        tbImportStatService.importerStats(tb, tb.getPlanId(), roundNum, stats);

        String message = "Import OK : TB " + tb.getInstanceId() + ", round " + roundNum + ", " + stats.size() + " stats lues";
        log.info(message);
        return message;
    }

    private void annulerTachePlanifiee() {
        ScheduledFuture<?> prev = tacheFinRoundPlanifiee.getAndSet(null);
        if (prev != null) {
            prev.cancel(false);
        }
    }

    private TerritoryBattle creerTerritoryBattle(JsonNode guild, JsonNode tbStatus, String instanceId) {
        // "TB_EVENT_TB3_MIXED:O1795456800000" -> "O1795456800000"
        String suffixe = instanceId.contains(":") ? instanceId.substring(instanceId.indexOf(':') + 1) : instanceId;

        Instant debut = null;
        Instant fin = null;
        for (JsonNode event : guild.path("guildEvents")) {
            if (!TB_EVENT_TYPE.equals(event.path("type").asText())) continue;
            for (JsonNode instance : event.path("instance")) {
                if (suffixe.equals(instance.path("id").asText())) {
                    debut = Instant.ofEpochMilli(instance.path("startTime").asLong());
                    fin = Instant.ofEpochMilli(instance.path("endTime").asLong());
                }
            }
        }

        String definitionId = tbStatus.path("definitionId").asText(null);
        Long planIdParDefaut = determinerPlanIdParDefaut(definitionId);

        TerritoryBattle tb = TerritoryBattle.builder()
                .guildId(guildId)
                .instanceId(instanceId)
                .definitionId(definitionId)
                .startTime(debut)
                .endTime(fin)
                .planId(planIdParDefaut)
                .build();

        log.info("Nouvelle TB détectée : {} (début {}, fin {}, planId par défaut {})",
                instanceId, debut, fin, planIdParDefaut);
        return territoryBattleRepository.save(tb);
    }

    private Long determinerPlanIdParDefaut(String definitionId) {
        return territoryBattleRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(tb -> tb.getPlanId() != null)
                .sorted((a, b) -> {
                    boolean aMemeType = definitionId != null && definitionId.equals(a.getDefinitionId());
                    boolean bMemeType = definitionId != null && definitionId.equals(b.getDefinitionId());
                    return Boolean.compare(bMemeType, aMemeType);
                })
                .map(TerritoryBattle::getPlanId)
                .findFirst()
                .orElse(null);
    }
}