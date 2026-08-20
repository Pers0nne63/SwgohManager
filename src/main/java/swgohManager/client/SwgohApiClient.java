package swgohManager.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.dto.EventsResponse;
import swgohManager.client.dto.GetEventsRequest;
import swgohManager.client.dto.GetLeaderboardRequest;
import swgohManager.client.dto.GuildRequest;
import swgohManager.client.dto.GuildResponse;
import swgohManager.client.dto.LeaderboardResponse;
import swgohManager.client.dto.PlayerRequest;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.service.PlayerIdentifier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwgohApiClient {

    private final WebClient swgohWebClient;

    public GuildResponse getGuild(String guildId) {
        log.debug("Appel API /guild pour guildId={}", guildId);
        return swgohWebClient.post()
                .uri("/guild")
                .bodyValue(GuildRequest.of(guildId))
                .retrieve()
                .bodyToMono(GuildResponse.class)
                .block();
    }
    
    public PlayerResponse getPlayer(PlayerIdentifier identifier) {
        PlayerRequest request;

        if (identifier instanceof PlayerIdentifier.ByPlayerId byId) {
            request = PlayerRequest.byPlayerId(byId.playerId());
        } else if (identifier instanceof PlayerIdentifier.ByAllyCode byAlly) {
            request = PlayerRequest.byAllyCode(byAlly.allyCode());
        } else {
            throw new IllegalArgumentException("Type d'identifiant joueur non supporté");
        }

        log.debug("Appel API /player avec {}", request);
        return swgohWebClient.post()
                .uri("/player")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PlayerResponse.class)
                .block();
    }

    public EventsResponse getEvents() {
        log.debug("Appel API /getevents");
        return swgohWebClient.post()
                .uri("/getevents")
                .bodyValue(new GetEventsRequest(false))
                .retrieve()
                .bodyToMono(EventsResponse.class)
                .block();
    }

    public LeaderboardResponse getLeaderboard(int leaderboardType, String eventInstanceId, String groupId) {
        GetLeaderboardRequest request = new GetLeaderboardRequest(
                new GetLeaderboardRequest.Payload(leaderboardType, eventInstanceId, groupId));

        log.debug("Appel API /getLeaderboard pour groupId={}", groupId);
        return swgohWebClient.post()
                .uri("/getLeaderboard")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LeaderboardResponse.class)
                .block();
    }
    

}

