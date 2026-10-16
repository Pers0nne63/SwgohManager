package swgohManager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.LeaderboardResponse;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.KyberStatLigne;
import swgohManager.model.KyberStatSnapshot;
import swgohManager.repository.KyberStatLigneRepository;
import swgohManager.repository.KyberStatSnapshotRepository;
import swgohManager.service.GacTopPlayerService.GacContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class KyberStatService {

    private static final int THREADS = 16;
    private static final int BATCH_SIZE = 64;
    private static final int MAX_BRACKETS = 20_000; // garde-fou anti-boucle infinie
    private static final int INTERVALLE_JOURS = 6;

    public String genererSnapshotSiNecessaire() {
        KyberStatSnapshot dernier = snapshotRepository.findFirstByOrderByDateSnapshotDesc();
        if (dernier != null && dernier.getDateSnapshot()
                .isAfter(LocalDateTime.now().minusDays(INTERVALLE_JOURS))) {
            return "Stats Kyber ignorées : dernier snapshot du " + dernier.getDateSnapshot().toLocalDate();
        }
        return genererSnapshot();
    }

    private record Sample(int division, int skillRating, long pg) {}

    private final SwgohApiClient apiClient;
    private final GacTopPlayerService gacTopPlayerService;
    private final KyberStatSnapshotRepository snapshotRepository;
    private final KyberStatLigneRepository ligneRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Parcourt tous les brackets Kyber, agrège en mémoire puis sauvegarde un snapshot.
     * Volontairement non transactionnel : seule la sauvegarde finale l'est.
     */
    public String  genererSnapshot() {
        GacContext ctx = gacTopPlayerService.resolveGacContext();
        Queue<Sample> samples = new ConcurrentLinkedQueue<>();
        AtomicInteger erreurs = new AtomicInteger();
        int brackets = 0;

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            boolean continuer = true;
            for (int debut = 0; continuer && debut < MAX_BRACKETS; debut += BATCH_SIZE) {
                List<CompletableFuture<Boolean>> futures = new ArrayList<>();
                for (int i = debut; i < debut + BATCH_SIZE; i++) {
                    final int bracket = i;
                    futures.add(CompletableFuture.supplyAsync(
                            () -> traiterBracket(ctx, bracket, samples, erreurs), pool));
                }
                int nonVides = (int) futures.stream()
                        .map(CompletableFuture::join)
                        .filter(b -> b)
                        .count();
                brackets += nonVides;
                continuer = nonVides > 0;
                log.info("Kyber stats : {} brackets traités, {} joueurs, {} erreurs",
                        brackets, samples.size(), erreurs.get());
            }
            
            
        } finally {
            pool.shutdown();
        }

        final int nbBrackets = brackets;
        final int nbErreurs = erreurs.get();
        transactionTemplate.executeWithoutResult(status -> sauvegarder(samples, nbBrackets, nbErreurs));

        log.info("Snapshot Kyber enregistré : {} joueurs, {} brackets, {} erreurs",
                samples.size(), nbBrackets, nbErreurs);
        
        String resultat = String.format("Snapshot Kyber enregistré : %d joueurs, %d brackets, %d erreurs",
                samples.size(), nbBrackets, nbErreurs);
        log.info(resultat);
        return resultat;
    }

    /** @return true si le bracket n'était pas vide */
    private boolean traiterBracket(GacContext ctx, int bracket, Queue<Sample> samples, AtomicInteger erreurs) {
        List<LeaderboardResponse.PlayerEntry> entries;
        try {
            LeaderboardResponse resp = apiClient.getLeaderboard(
                    GacTopPlayerService.LEADERBOARD_TYPE, ctx.eventInstanceId(), ctx.groupId(bracket));
            if (resp == null || resp.leaderboard() == null || resp.leaderboard().isEmpty()
                    || resp.leaderboard().get(0).player() == null
                    || resp.leaderboard().get(0).player().isEmpty()) {
                return false;
            }
            entries = resp.leaderboard().get(0).player();
        } catch (Exception e) {
            log.warn("Bracket {} en erreur : {}", bracket, e.getMessage());
            erreurs.incrementAndGet();
            return false;
        }

        for (LeaderboardResponse.PlayerEntry entry : entries) {
            try {
                PlayerResponse p = apiClient.getPlayer(new PlayerIdentifier.ByPlayerId(entry.id()));
                if (p.playerRating() == null
                        || p.playerRating().playerSkillRating() == null
                        || p.playerRating().playerSkillRating().skillRating() == null
                        || p.playerRating().playerRankStatus() == null) {
                    continue;
                }
                PlayerResponse.PlayerRankStatus rank = p.playerRating().playerRankStatus();
                if (!"KYBER".equals(rank.leagueId()) || rank.divisionId() == null) {
                    continue;
                }

                int division = 6 - rank.divisionId() / 5; // 25 -> 1 ... 5 -> 5 (à vérifier)
                if (division < 1 || division > 5) {
                    continue;
                }

                samples.add(new Sample(
                        division,
                        p.playerRating().playerSkillRating().skillRating(),
                        parseLong(entry.power())));
            } catch (Exception e) {
                erreurs.incrementAndGet();
                log.debug("Joueur {} ignoré : {}", entry.id(), e.getMessage());
            }
        }
        return true;
    }

    private void sauvegarder(Queue<Sample> samples, int brackets, int erreurs) {
        KyberStatSnapshot snap = new KyberStatSnapshot();
        snap.setDateSnapshot(LocalDateTime.now());
        snap.setNbJoueurs(samples.size());
        snap.setNbBrackets(brackets);
        snap.setNbErreurs(erreurs);
        snap = snapshotRepository.save(snap);

        List<KyberStatLigne> lignes = new ArrayList<>();
        lignes.addAll(agreger(snap.getId(), "DIVISION", samples, Sample::division));
        lignes.addAll(agreger(snap.getId(), "TRANCHE_SR", samples, s -> (s.skillRating() / 100) * 100));
        ligneRepository.saveAll(lignes);
    }

    private List<KyberStatLigne> agreger(Long snapshotId, String type, Collection<Sample> samples,
                                         ToIntFunction<Sample> cleFn) {
        return samples.stream()
                .collect(Collectors.groupingBy(cleFn::applyAsInt))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    KyberStatLigne l = new KyberStatLigne();
                    l.setSnapshotId(snapshotId);
                    l.setType(type);
                    l.setCle(e.getKey());
                    l.setNbJoueurs(e.getValue().size());
                    l.setPgMoyenne(Math.round(
                            e.getValue().stream().mapToLong(Sample::pg).average().orElse(0)));
                    return l;
                })
                .toList();
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