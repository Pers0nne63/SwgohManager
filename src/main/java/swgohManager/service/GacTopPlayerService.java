package swgohManager.service;

import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.EventsResponse;
import swgohManager.client.dto.LeaderboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GacTopPlayerService {

    private final SwgohApiClient swgohApiClient;

    private static final String NOM_EVENT_GAC = "TERRITORY_TOURNAMENT_EVENT_NAME";
    private static final String LIGUE = "KYBER";
    private static final long PUISSANCE_MINIMUM = 13_000_000L;
    private static final int NB_JOUEURS_CIBLE = 100;
    private static final int MAX_BRACKETS = 50; // garde-fou anti-boucle infinie
    public static final int LEADERBOARD_TYPE = 4;

    public record GacContext(String gacId, String eventInstanceId) {
        public String groupId(int bracket) {
            return gacId + ":" + eventInstanceId + ":" + LIGUE + ":" + bracket;
        }
    }

    public List<String> recupererTopPlayers() {
        GacContext ctx = resolveGacContext();
        LinkedHashSet<String> topPlayers = new LinkedHashSet<>();

        int i = 0;
        while (topPlayers.size() < NB_JOUEURS_CIBLE && i < MAX_BRACKETS) {
            LeaderboardResponse bracket = swgohApiClient.getLeaderboard(
                    LEADERBOARD_TYPE, ctx.eventInstanceId(), ctx.groupId(i));

            if (bracket == null || bracket.leaderboard() == null || bracket.leaderboard().isEmpty()
                    || bracket.leaderboard().get(0).player() == null
                    || bracket.leaderboard().get(0).player().isEmpty()) {
                break; // bracket vide : il n'y en a plus après
            }

            bracket.leaderboard().get(0).player().stream()
                    .filter(p -> parseLong(p.power()) > PUISSANCE_MINIMUM)
                    .map(LeaderboardResponse.PlayerEntry::id)
                    .forEach(topPlayers::add);

            i++;
        }

        if (topPlayers.isEmpty()) {
            log.warn("Aucun joueur top GAC trouvé après {} bracket(s) parcouru(s)", i);
            return List.of();
        }

        log.info("{} joueur(s) top GAC récupéré(s) après {} bracket(s) parcouru(s)", topPlayers.size(), i);
        return new ArrayList<>(topPlayers);
    }
    
    public GacContext resolveGacContext() {
        EventsResponse events = swgohApiClient.getEvents();

        EventsResponse.GameEvent gac = events.gameEvent().stream()
                .filter(e -> NOM_EVENT_GAC.equals(e.nameKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune GAC en cours trouvée (" + NOM_EVENT_GAC + ")"));

        if (gac.instance() == null || gac.instance().isEmpty()) {
            throw new IllegalStateException("La GAC trouvée n'a pas d'instance active");
        }
        return new GacContext(gac.id(), gac.instance().get(0).id());
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}