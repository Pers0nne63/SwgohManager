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
import swgohManager.model.RaidHistorique;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.RaidHistoriqueRepository;
import swgohManager.repository.TerritoryBattleRepository;
import swgohManager.repository.PlayerRatingHistoriqueRepository;

@Service
@RequiredArgsConstructor
public class GuildOverviewService {

    private final JoueurRepository joueurRepository;
    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final RaidHistoriqueRepository raidHistoriqueRepository;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final FarmPlanProgressService farmPlanProgressService;
    private final PlayerRatingHistoriqueRepository playerRatingHistoriqueRepository;

    // 1. Ajout de ratingActuel dans le record PlayerRow
    public record PlayerRow(
            String playerId, 
            String playerName, 
            Long galacticPower, 
            String leagueId, 
            Double modQ, 
            Double farmPlanPourcentage,
            Integer ratingActuel
    ) {}

    public record RaidSummary(Instant endTime, long totalScore, int nbParticipants) {}
    public record TbSummary(String definitionId, Instant endTime, Integer totalStars) {}
    
    public List<PlayerRow> getJoueurs() {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();

        List<String> playerIds = joueurs.stream().map(Joueur::getPlayerId).toList();

        Map<String, FarmPlanProgressService.PlayerFarmProgress> progressions = farmPlanProgressService
                .getProgressionPourJoueurs(playerIds);
        
        // 2. Récupération des derniers ratings sous forme de Map<playerId, Double>
        Map<String, Integer> ratingsMap = playerRatingHistoriqueRepository
                .findDernierRatingPourJoueurs(playerIds)
                .stream()
                .collect(Collectors.toMap(
                        PlayerRatingHistoriqueRepository.DernierRatingProjection::getPlayerId,
                        PlayerRatingHistoriqueRepository.DernierRatingProjection::getRating,
                        (r1, r2) -> r1 // En cas de doublon sur la même date
                ));

        return joueurs.stream()
                .sorted(Comparator.comparing(Joueur::getGalacticPower, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(j -> new PlayerRow(
                        j.getPlayerId(),
                        j.getPlayerName(),
                        j.getGalacticPower(),
                        j.getLeagueId(),
                        playerModQActuelRepository.findByPlayerId(j.getPlayerId())
                                .map(PlayerModQActuel::getModQ).orElse(null),
                        progressions.get(j.getPlayerId()).pourcentage(),
                        ratingsMap.get(j.getPlayerId()) // 3. Transmission du rating
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