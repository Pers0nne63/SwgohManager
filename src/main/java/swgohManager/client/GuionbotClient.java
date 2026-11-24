package swgohManager.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class GuionbotClient {

    private final RestClient restClient;
    private final boolean apiKeyConfigured;

    public GuionbotClient(@Value("${app.guionbot.base-url}") String baseUrl,
                          @Value("${app.guionbot.api-key:}") String apiKey,
                          @Value("${app.guionbot.timeout-seconds:180}") int timeoutSeconds) {
        this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Tableau de {mapStatId, playerStat[]} (cumul de toute la TB). */
    public JsonNode fetchMapStats() {
        return post("/tb-mapstats.php");
    }

    /** Objet guilde du jeu (profile, member, territoryBattleStatus, guildEvents...). */
    public JsonNode fetchGuild() {
        return post("/guild.php");
    }

    private JsonNode post(String path) {
        if (!apiKeyConfigured) {
            throw new IllegalStateException(
                    "Clé API guionbot absente : définir la variable d'environnement GUIONBOT_API_KEY");
        }
        try {
            JsonNode body = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null || !body.path("success").asBoolean(false)) {
                throw new IllegalStateException("Réponse guionbot en échec pour " + path + " : "
                        + (body == null ? "corps vide" : body.path("error").asText("sans message")));
            }
            return body.path("data");
        } catch (RestClientResponseException e) {
            String retryAfter = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("Retry-After") : null;
            throw new IllegalStateException("guionbot " + path + " : HTTP " + e.getStatusCode().value()
                    + (retryAfter != null ? " (Retry-After " + retryAfter + " s)" : ""), e);
        }
    }
}