package swgohManager.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.GuionbotClient;
import swgohManager.model.TbEvent;
import swgohManager.repository.TbEventRepository;

/**
 * Import des événements de zone de TB (guionbot tb-events.php).
 * Format reçu : { instanceId -> { eventId -> événement } }.
 * Insert-only et idempotent : un événement déjà connu (même id) est ignoré, on peut donc
 * rappeler l'import autant de fois que voulu (l'API ne renvoie qu'une fenêtre des derniers événements).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TbEventImportService {

    private static final String PREFIX = "TERRITORY_CHANNEL_ACTIVITY_";
    private static final Pattern ZONE = Pattern.compile("_phase(\\d+)_conflict(\\d+)(_bonus)?$");
    private static final Pattern GUILD = Pattern.compile("^TZ-\\{([^}]+)\\}-");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    private final GuionbotClient guionbotClient;
    private final TbEventRepository repository;

    private static final DateTimeFormatter FORMAT_LOG =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Europe/Paris"));

    /** Points de zone perdus entre deux événements connus (debut = dernier connu avant le trou, fin = premier connu après). */
    public record Trou(String zoneId, Instant debut, Instant fin, long pointsManquants) {}

    public record Resultat(int lus, int nouveaux, int dejaPresents, int ignores, List<Trou> trous) {}

    /** Appelle guionbot (1 appel) puis importe. */
    public Resultat importerDepuisApi() {
        return importer(guionbotClient.fetchTbEvents());
    }

    /** Importe le noeud "data" de la réponse (utilisable aussi avec un fichier JSON déjà lu). */
    @Transactional
    public Resultat importer(JsonNode data) {
        int lus = 0;
        int dejaPresents = 0;
        int ignores = 0;
        List<TbEvent> nouveaux = new ArrayList<>();

        for (Map.Entry<String, JsonNode> instance : data.properties()) {
            String instanceId = instance.getKey();
            Set<String> existants = repository.findEventIdsByInstanceId(instanceId);

            for (Map.Entry<String, JsonNode> entry : instance.getValue().properties()) {
                lus++;
                JsonNode ev = entry.getValue();
                String eventId = ev.path("id").asText(entry.getKey());
                if (existants.contains(eventId)) {
                    dejaPresents++;
                    continue;
                }
                TbEvent event = parse(instanceId, eventId, ev);
                if (event == null) {
                    ignores++;
                    continue;
                }
                nouveaux.add(event);
            }
        }

        repository.saveAll(nouveaux);
        repository.flush();
        List<Trou> trous = detecterTrous(nouveaux);
        log.info("tb-events : {} lus, {} nouveaux, {} déjà en base, {} ignorés, {} trou(s)",
                lus, nouveaux.size(), dejaPresents, ignores, trous.size());
        return new Resultat(lus, nouveaux.size(), dejaPresents, ignores, trous);
    }

    /**
     * Trous entre ce lot et ce qui était déjà en base : on ne garde que les trous dont l'événement
     * suivant vient d'être inséré, pour ne pas re-signaler à chaque appel les trous des appels précédents.
     */
    private List<Trou> detecterTrous(List<TbEvent> nouveaux) {
        List<Trou> trous = new ArrayList<>();
        if (nouveaux.isEmpty()) {
            return trous;
        }
        Set<String> idsNouveaux = nouveaux.stream().map(TbEvent::getEventId).collect(Collectors.toSet());
        for (String instanceId : nouveaux.stream().map(TbEvent::getInstanceId).distinct().toList()) {
            for (TbEventRepository.TrouProjection t : repository.findTrousByInstanceId(instanceId)) {
                if (!idsNouveaux.contains(t.getEventId())) {
                    continue;
                }
                Trou trou = new Trou(t.getZoneId(), Instant.ofEpochMilli(t.getDebutMs()),
                        Instant.ofEpochMilli(t.getFinMs()), t.getPointsManquants());
                trous.add(trou);
                log.warn("tb-events : trou zone {} entre {} et {} ({} points manquants)",
                        trou.zoneId(), FORMAT_LOG.format(trou.debut()), FORMAT_LOG.format(trou.fin()),
                        trou.pointsManquants());
            }
        }
        return trous;
    }

    private TbEvent parse(String instanceId, String eventId, JsonNode ev) {
        // "data" est un tableau ; il ne contient qu'un élément dans tous les exemples reçus
        JsonNode activityNode = ev.path("data").path(0);
        JsonNode zone = activityNode.path("activity").path("zoneData");
        if (zone.isMissingNode()) {
            log.warn("tb-events : événement {} ignoré (activityType={} sans zoneData)",
                    eventId, activityNode.path("activityType").asText("?"));
            return null;
        }

        TbEvent e = new TbEvent();
        e.setEventId(eventId);
        e.setInstanceId(instanceId);
        e.setEventType(ev.path("eventType").asText(null));
        e.setActivityType(activityNode.path("activityType").asText(null));
        e.setEventTime(Instant.ofEpochMilli(ev.path("timestamp").asLong()));
        e.setAuthorId(ev.path("authorId").asText(null));
        e.setAuthorName(ev.path("authorName").asText(null));

        Matcher g = GUILD.matcher(ev.path("channelId").asText(""));
        if (g.find()) {
            e.setGuildId(g.group(1));
        }

        e.setMapId(zone.path("mapId").asText(null));
        String zoneId = zone.path("zoneId").asText(null);
        e.setZoneId(zoneId);
        if (zoneId != null) {
            Matcher z = ZONE.matcher(zoneId);
            if (z.find()) {
                e.setPhase(Integer.valueOf(z.group(1)));
                e.setConflict(Integer.valueOf(z.group(2)));
                e.setBonus(z.group(3) != null);
            }
        }

        e.setState(intOrNull(zone.path("state")));
        e.setScoreDelta(longOrNull(zone.path("scoreDelta")));
        e.setScoreTotal(longOrNull(zone.path("scoreTotal")));
        e.setParticipationDelta(intOrNull(zone.path("participationDelta")));
        e.setParticipationTotal(intOrNull(zone.path("participationTotal")));

        String source = zone.path("sourceZoneId").asText("");
        if (!source.isEmpty()) {
            e.setSourceZoneId(source);
            Matcher n = TRAILING_NUMBER.matcher(source);
            if (n.find()) {
                e.setSourceNumero(Integer.valueOf(n.group(1)));
            }
        }

        JsonNode message = zone.path("activityLogMessage");
        String key = message.path("key").asText("");
        JsonNode params = message.path("param");
        e.setMessageKey(key);

        String court = key.startsWith(PREFIX) ? key.substring(PREFIX.length()) : key;
        switch (court) {
            case "CONFLICT_DEPLOY" -> e.setType(TbEvent.Type.DEPLOY);
            case "STARS_GAINED" -> {
                e.setType(TbEvent.Type.STARS);
                e.setEtoiles(paramInt(params, 0));
            }
            case "STRIKE_LINKED_CONFLICT_CONTRIBUTION_ALL_COMPLETE", "STRIKE_LINKED_CONFLICT_CONTRIBUTION" -> {
                e.setType(court.endsWith("ALL_COMPLETE") ? TbEvent.Type.STRIKE_COMPLETE : TbEvent.Type.STRIKE_PARTIAL);
                e.setMissionNameKey(params.path(1).path("key").asText(null));
                e.setMissionFaites(paramInt(params, 2));
                e.setMissionTotal(paramInt(params, 3));
            }
            case "COVERT_COMPLETE" -> {
                e.setType(TbEvent.Type.COVERT);
                e.setMissionNameKey(params.path(0).path("key").asText(null));
            }
            default -> {
                e.setType(TbEvent.Type.AUTRE);
                log.warn("tb-events : clé de message inconnue '{}' (événement {})", key, eventId);
            }
        }
        return e;
    }

    /** param[index].paramValue[0] converti en entier (null si absent ou non numérique). */
    private static Integer paramInt(JsonNode params, int index) {
        JsonNode v = params.path(index).path("paramValue").path(0);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        try {
            return Integer.valueOf(v.asText().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer intOrNull(JsonNode n) {
        return n.isMissingNode() || n.isNull() ? null : n.asInt();
    }

    private static Long longOrNull(JsonNode n) {
        return n.isMissingNode() || n.isNull() ? null : n.asLong();
    }
}