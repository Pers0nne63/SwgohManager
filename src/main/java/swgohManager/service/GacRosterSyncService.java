package swgohManager.service;

import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.PlayerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GacRosterSyncService {

    private final GacTopPlayerService gacTopPlayerService;
    private final SwgohApiClient swgohApiClient;
    private final LeaderboardRosterService leaderboardRosterService;

    @Qualifier("playerSyncExecutor")
    private final ExecutorService playerSyncExecutor;

    public GacSyncResult synchroniserTopPlayers() {
        List<String> playerIds = gacTopPlayerService.recupererTopPlayers();
        log.info("Synchronisation du roster GAC de {} joueur(s), 10 en parallèle", playerIds.size());

        List<CompletableFuture<Boolean>> futures = playerIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> synchroniserUnJoueur(id), playerSyncExecutor))
                .toList();

        long succes = futures.stream().map(CompletableFuture::join).filter(Boolean::booleanValue).count();
        int echecs = playerIds.size() - (int) succes;

        String resume = String.format("%d joueur(s) synchronisé(s), %d échec(s) sur %d joueur(s) top GAC",
                succes, echecs, playerIds.size());
        log.info(resume);

        return new GacSyncResult(playerIds.size(), (int) succes, echecs);
    }

    private boolean synchroniserUnJoueur(String playerId) {
        try {
            PlayerResponse response = swgohApiClient.getPlayer(PlayerIdentifier.of(playerId, null));
            leaderboardRosterService.enregistrerRoster(response);
            return true;
        } catch (Exception e) {
            log.error("Échec de synchronisation GAC pour le joueur {} : {}", playerId, e.getMessage());
            return false;
        }
    }

    public record GacSyncResult(int totalJoueurs, int succes, int echecs) {}
}