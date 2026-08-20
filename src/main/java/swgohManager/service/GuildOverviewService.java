package swgohManager.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.model.PlayerModQActuel;
import swgohManager.model.PlayerStatqActuel;
import swgohManager.model.RaidHistorique;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerRatingHistoriqueRepository;
import swgohManager.repository.PlayerStatqActuelRepository;
import swgohManager.repository.RaidHistoriqueRepository;
import swgohManager.repository.TerritoryBattleRepository;

@Service
@RequiredArgsConstructor
public class GuildOverviewService {

    private final JoueurRepository joueurRepository;
    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final RaidHistoriqueRepository raidHistoriqueRepository;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final FarmPlanProgressService farmPlanProgressService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final PlayerRatingHistoriqueRepository playerRatingHistoriqueRepository;
    private final PlayerStatqActuelRepository playerStatqActuelRepository;
    
    public record PlayerRow(
            String playerId,
            String playerName,
            Long galacticPower,
            String leagueId,
            Double modQ,
            Double farmPlanPourcentage,
            Integer ratingActuel,
            Double omicronP1Pourcentage,
            Double statQ
    ) {}

    public record RaidSummary(Instant endTime, long totalScore, int nbParticipants) {}
    public record TbSummary(String definitionId, Instant endTime, Integer totalStars) {}

    public List<PlayerRow> getJoueurs() {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();
        List<String> playerIds = joueurs.stream().map(Joueur::getPlayerId).toList();

        // Requêtes "Batch" (1 seule requête SQL par sujet)
        Map<String, Double> farmPlanPourcentages = farmPlanProgressService.getPourcentagesPourJoueurs(playerIds);
        Map<String, Double> omicronP1Pourcentages = omicronPlanProgressService.getPourcentagesOmiPourJoueurs(playerIds);

        Map<String, Double> modQMap = playerModQActuelRepository.findByPlayerIdIn(playerIds).stream()
                .collect(Collectors.toMap(PlayerModQActuel::getPlayerId, PlayerModQActuel::getModQ, (m1, m2) -> m1));

        Map<String, Integer> ratingsMap = playerRatingHistoriqueRepository
                .findDernierRatingPourJoueurs(playerIds)
                .stream()
                .collect(Collectors.toMap(
                        PlayerRatingHistoriqueRepository.DernierRatingProjection::getPlayerId,
                        PlayerRatingHistoriqueRepository.DernierRatingProjection::getRating,
                        (r1, r2) -> r1
                ));
        
        Map<String, Double> statQMap = playerStatqActuelRepository.findByPlayerIdIn(playerIds).stream()
                .collect(Collectors.toMap(PlayerStatqActuel::getPlayerId, PlayerStatqActuel::getStatq, (a, b) -> a));
        
        return joueurs.stream()
                .sorted(Comparator.comparing(Joueur::getGalacticPower, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(j -> new PlayerRow(
                        j.getPlayerId(),
                        j.getPlayerName(),
                        j.getGalacticPower(),
                        j.getLeagueId(),
                        modQMap.get(j.getPlayerId()),
                        farmPlanPourcentages.get(j.getPlayerId()),
                        ratingsMap.get(j.getPlayerId()),
                        omicronP1Pourcentages.get(j.getPlayerId()),
                        statQMap.get(j.getPlayerId())
                		))
                .toList();
    }

    public RaidSummary getDernierRaid() {
        return raidHistoriqueRepository.findTopByOrderByEndTimeDesc()
                .map(dernier -> {
                    List<RaidHistorique> lignes = raidHistoriqueRepository.findByEndTime(dernier.getEndTime());
                    long total = lignes.stream().mapToLong(r -> r.getScore() != null ? r.getScore() : 0L).sum();
                    return new RaidSummary(dernier.getEndTime(), total, lignes.size());
                })
                .orElse(null);
    }

    public TbSummary getDerniereTb() {
        return territoryBattleRepository.findTopByOrderByEndTimeDesc()
                .map(tb -> new TbSummary(tb.getDefinitionId(), tb.getEndTime(), tb.getTotalStars()))
                .orElse(null);
    }
}