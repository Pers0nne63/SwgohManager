package swgohManager.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.dto.RelicTierDefinitionRaw;
import swgohManager.client.dto.UnitRaw;
import swgohManager.client.dto.UnitSegmentData;

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
    
    public List<swgohManager.client.dto.CategoryRaw> getCategories(String version) {
        return streamSegment(version, 1, "category", swgohManager.client.dto.CategoryRaw.class);
    }

    public List<swgohManager.client.dto.BattleTargetingRuleRaw> getBattleTargetingRules(String version) {
        return streamSegment(version, 1, "battleTargetingRule", swgohManager.client.dto.BattleTargetingRuleRaw.class);
    }

    public List<swgohManager.client.dto.StatProgressionRaw> streamStatProgressionSegment(String version) {
        return streamSegment(version, 2, "statProgression", swgohManager.client.dto.StatProgressionRaw.class);
    }
    
    public List<swgohManager.client.dto.AbilityRaw> getAbilities(String version) {
        return streamSegment(version, 2, "ability", swgohManager.client.dto.AbilityRaw.class);
    }
    
    public List<swgohManager.client.dto.DatacronTemplateRaw> getDatacronTemplates(String version) {
        return streamSegment(version, 4, "datacronTemplate", swgohManager.client.dto.DatacronTemplateRaw.class);
    }

    public List<swgohManager.client.dto.DatacronAffixTemplateSetRaw> getDatacronAffixes(String version) {
        return streamSegment(version, 4, "datacronAffixTemplateSet", swgohManager.client.dto.DatacronAffixTemplateSetRaw.class);
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
    
    public String getLatestLocalizationVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/metadata"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         // 1. On vérifie d'abord si la requête a réussi (Code HTTP 200 OK)
         if (response.statusCode() != 200) {
             log.error("Erreur de l'API SWGOH. Code HTTP: {}. Corps de la réponse: {}", 
                       response.statusCode(), response.body());
             throw new RuntimeException("Impossible de récupérer la version, l'API a retourné une erreur HTTP " + response.statusCode());
         }

         var node = objectMapper.readTree(response.body());

         // 2. On utilise .path() pour éviter le NullPointerException
         var versionNode = node.path("latestLocalizationBundleVersion");

         // 3. On vérifie si la clé existe vraiment dans le JSON
         if (versionNode.isMissingNode() || versionNode.isNull()) {
             log.error("La clé 'latestLocalizationBundleVersion' est absente. Le JSON reçu était : {}", response.body());
             throw new RuntimeException("Format de réponse API SWGOH inattendu.");
         }

         String version = versionNode.asText();
         log.info("Version de localisation récupérée : {}", version);
         return version;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new SwgohDataException("Impossible de récupérer la version de localisation (/metadata)", e);
        }
    }
    
    public java.util.Map<String, String> getLocalizationFrancaise(String version) {
        String id = version + ":FRE_FR";
        String body = """
                {"payload":{"id":"%s"},"unzip":true}
                """.formatted(id);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/localization"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMinutes(2))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream in = response.body()) {
                return extraireTraductionsFrancaises(in);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new SwgohDataException("Erreur lors du streaming de la localisation française", e);
        }
    }

    private java.util.Map<String, String> extraireTraductionsFrancaises(InputStream in) throws IOException {
        java.util.Map<String, String> traductions = new java.util.HashMap<>();

        try (JsonParser parser = objectMapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Réponse inattendue : pas un objet JSON");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String champ = parser.currentName();
                parser.nextToken();

                if ("Loc_FRE_FR.txt".equals(champ)) {
                    String contenu = parser.getValueAsString();
                    for (String ligne : contenu.split("\n")) {
                        int idx = ligne.indexOf('|');
                        if (idx > 0) {
                            String cle = ligne.substring(0, idx).trim();
                            String valeur = ligne.substring(idx + 1).trim();
                            traductions.put(cle, valeur);
                        }
                    }
                } else {
                    parser.skipChildren();
                }
            }
        }

        log.info("{} traduction(s) française(s) extraite(s)", traductions.size());
        return traductions;
    }
}