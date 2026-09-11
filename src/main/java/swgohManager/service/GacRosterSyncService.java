package swgohManager.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.PlayerResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class GacRosterSyncService {

    private final GacTopPlayerService gacTopPlayerService;
    private final SwgohApiClient swgohApiClient;
    private final LeaderboardRosterService leaderboardRosterService;
    private final SyncProgressService progressService;

    @Qualifier("playerSyncExecutor")
    private final ExecutorService playerSyncExecutor;

    public GacSyncResult synchroniserTopPlayers(boolean withProgress) {
    	List<String> playerIds = gacTopPlayerService.recupererTopPlayers();
        int totalJoueurs = playerIds.size();
        log.info("Synchronisation du roster GAC de {} joueur(s), 10 en parallèle", totalJoueurs);

        AtomicInteger jouersTraites = new AtomicInteger(0);

        List<CompletableFuture<Boolean>> futures = playerIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> synchroniserUnJoueur(id), playerSyncExecutor)
                        .thenApply(succes -> {
                            int traites = jouersTraites.incrementAndGet();
                            if (withProgress && totalJoueurs > 0) {
                                // Plage de 0% à 80% pour le GAC
                                int percent =  (int) ((traites / (double) totalJoueurs) * 80);
                                String step = String.format("Rosters GAC (%d/%d)", traites, totalJoueurs);
                                String msg = String.format("Joueur GAC %d/%d synchronisé", traites, totalJoueurs);
                                progressService.notifyProgress("gac", percent, step, msg);
                            }
                            return succes;
                        }))
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