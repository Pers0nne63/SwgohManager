package swgohManager.client;

import swgohManager.client.dto.UnitRaw;
import swgohManager.client.dto.RelicTierDefinitionRaw;
import swgohManager.client.dto.UnitSegmentData;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwgohDataClient {

    @Value("${swgoh.api.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String getLatestGameVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/metadata"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var node = objectMapper.readTree(response.body());
            String version = node.get("latestGamedataVersion").asText();
            log.info("Version du jeu récupérée : {}", version);
            return version;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new SwgohDataException("Impossible de récupérer la version du jeu (/metadata)", e);
        }
    }

    public List<swgohManager.client.dto.SkillRaw> streamSkillSegment(String version) {
        return streamSegment(version, 1, "skill", swgohManager.client.dto.SkillRaw.class);
    }

    public List<swgohManager.client.dto.StatProgressionRaw> streamStatProgressionSegment(String version) {
        return streamSegment(version, 2, "statProgression", swgohManager.client.dto.StatProgressionRaw.class);
    }

    /**
     * Récupère un segment de /data et n'extrait QUE le champ demandé, en streaming,
     * sans jamais charger le document complet (~150 Mo) en mémoire.
     */
    private <T> List<T> streamSegment(String version, int segment, String champ, Class<T> type) {
        String body = """
                {"payload":{"version":"%s","includePveUnits":false,"requestSegment":%d}}
                """.formatted(version, segment);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream in = response.body()) {
                return extraireListe(in, champ, type);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new SwgohDataException("Erreur lors du streaming du segment " + segment + " (" + champ + ")", e);
        }
    }

    private <T> List<T> extraireListe(InputStream in, String champRecherche, Class<T> type) throws IOException {
        List<T> resultat = new ArrayList<>();

        try (JsonParser parser = objectMapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Réponse inattendue : le document ne commence pas par un objet JSON");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                parser.nextToken();

                if (champRecherche.equals(champ)) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        resultat.add(objectMapper.readValue(parser, type));
                    }
                } else {
                    parser.skipChildren();
                }
            }
        }

        log.info("{} élément(s) '{}' extrait(s) en streaming", resultat.size(), champRecherche);
        return resultat;
    }

    public static class SwgohDataException extends RuntimeException {
        public SwgohDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public UnitSegmentData streamUnitSegment(String version) {
        String body = """
                {"payload":{"version":"%s","includePveUnits":false,"requestSegment":3}}
                """.formatted(version);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/data"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMinutes(3))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream in = response.body()) {
                return extraireUnitsEtRelicTier(in);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new SwgohDataException("Erreur lors du streaming du segment 3 (units/relicTierDefinition)", e);
        }
    }

    private UnitSegmentData extraireUnitsEtRelicTier(InputStream in) throws IOException {
        List<UnitRaw> units = new ArrayList<>();
        List<RelicTierDefinitionRaw> relics = new ArrayList<>();

        try (JsonParser parser = objectMapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Réponse inattendue : pas un objet JSON");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                parser.nextToken();

                switch (champ) {
                    case "units" -> {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            units.add(objectMapper.readValue(parser, UnitRaw.class));
                        }
                    }
                    case "relicTierDefinition" -> {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            relics.add(objectMapper.readValue(parser, RelicTierDefinitionRaw.class));
                        }
                    }
                    default -> parser.skipChildren();
                }
            }
        }

        log.info("{} unité(s) et {} relicTierDefinition extraite(s) en streaming", units.size(), relics.size());
        return new UnitSegmentData(units, relics);
    }
}