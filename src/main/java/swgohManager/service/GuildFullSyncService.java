package swgohManager.service;

import swgohManager.model.Joueur;
import swgohManager.model.SyncExecution;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.SyncExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildFullSyncService {

    private final GuildSyncService guildSyncService;
    private final JoueurRepository joueurRepository;
    private final PlayerSyncService playerSyncService;
    private final SyncExecutionRepository syncExecutionRepository;

    @Qualifier("playerSyncExecutor")
    private final ExecutorService playerSyncExecutor;

    public GuildFullSyncResult synchroniserGuildeComplete() {
        GuildSyncService.GuildSyncResult guildResult = guildSyncService.synchroniserGuilde();

        // Un seul idSync pour l'ensemble des joueurs de ce lot
        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();

        List<Joueur> joueursPresents = joueurRepository.findAllByPresentInGuildTrue();
        log.info("Synchronisation de {} joueur(s) sous idSync={}, 10 en parallèle", joueursPresents.size(), idSync);

        List<CompletableFuture<SyncOutcome>> futures = joueursPresents.stream()
                .map(joueur -> CompletableFuture.supplyAsync(
                        () -> synchroniserUnJoueur(joueur, idSync), playerSyncExecutor))
                .toList();

        List<String> succes = new ArrayList<>();
        List<String> echecs = new ArrayList<>();

        for (CompletableFuture<SyncOutcome> future : futures) {
            SyncOutcome outcome = future.join();
            if (outcome.succes()) {
                succes.add(outcome.playerId());
            } else {
                echecs.add(outcome.playerId());
            }
        }

        String resume = String.format("%d joueur(s) synchronisé(s) avec succès, %d échec(s) (idSync=%d)",
                succes.size(), echecs.size(), idSync);
        log.info(resume);

        return new GuildFullSyncResult(guildResult, succes.size(), echecs, resume);
    }

    private SyncOutcome synchroniserUnJoueur(Joueur joueur, Long idSync) {
        try {
            PlayerIdentifier identifier = PlayerIdentifier.of(joueur.getPlayerId(), null);
            playerSyncService.synchroniserJoueur(identifier, idSync);
            return new SyncOutcome(joueur.getPlayerId(), true);
        } catch (Exception e) {
            log.error("Échec de synchronisation du joueur {} ({}) : {}",
                    joueur.getPlayerName(), joueur.getPlayerId(), e.getMessage());
            return new SyncOutcome(joueur.getPlayerId(), false);
        }
    }

    private record SyncOutcome(String playerId, boolean succes) {}

    public record GuildFullSyncResult(
            GuildSyncService.GuildSyncResult guildSync,
            int joueursSynchronises,
            List<String> joueursEnEchec,
            String resume
    ) {}
}