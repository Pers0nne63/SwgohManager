package swgohManager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.RelicR8CountProjection;
import swgohManager.controller.dto.TbSyntheseRoundProjection;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.TbScoreJoueurRepository;

@Service
@RequiredArgsConstructor
public class TbSyntheseService {

    private static final int[] ROUNDS = {3, 4, 5, 6};

    private static final Map<Integer, Double> POIDS_COMBAT = Map.of(3, 0.0, 4, 0.0, 5, 0.0, 6, 0.0);
    private static final Map<Integer, Double> POIDS_VAGUE  = Map.of(3, 1.0, 4, 2.0, 5, 3.0, 6, 4.0);

    private final TbScoreJoueurRepository tbScoreJoueurRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final JoueurRepository joueurRepository;

    public record BtCellule(Long tbId, LocalDateTime endTime, long vagues, long combats) {}
    public record RoundColonne(int roundNum, List<BtCellule> historique) {}
    public record JoueurSynthese(String playerId, String playerName, Double note, long nbR8Plus, List<RoundColonne> rounds) {}
    public record GuildeRoundMoyenne(int roundNum, double moyenneCombats, double moyenneVagues) {}
    public record ChartPoint(String playerName, Double note, long nbR8Plus) {}
    public record SyntheseResult(List<GuildeRoundMoyenne> guilde, List<JoueurSynthese> joueurs, List<ChartPoint> pointsGraphique) {}

    public SyntheseResult calculerSynthese() {
        List<TbSyntheseRoundProjection> lignes = tbScoreJoueurRepository.findSyntheseJoueursDernieresBt();

        Map<Long, LocalDateTime> btEndTimes = new LinkedHashMap<>();
        for (TbSyntheseRoundProjection l : lignes) {
            btEndTimes.putIfAbsent(l.getTerritoryBattleId(), l.getEndTime());
        }
        List<Long> tbIdsOrdonnes = btEndTimes.keySet().stream()
                .sorted(Comparator.comparing(btEndTimes::get))
                .toList();

        Map<String, Map<Integer, Map<Long, long[]>>> donnees = new LinkedHashMap<>();
        Map<String, Set<Long>> btAvecDonneesParJoueur = new HashMap<>();

        for (TbSyntheseRoundProjection l : lignes) {
            donnees
                .computeIfAbsent(l.getPlayerId(), k -> new HashMap<>())
                .computeIfAbsent(l.getRoundNum(), k -> new LinkedHashMap<>())
                .put(l.getTerritoryBattleId(), new long[]{nz(l.getVagues()), nz(l.getCombats())});
            btAvecDonneesParJoueur.computeIfAbsent(l.getPlayerId(), k -> new HashSet<>()).add(l.getTerritoryBattleId());
        }

        List<GuildeRoundMoyenne> guilde = new ArrayList<>();
        for (int round : ROUNDS) {
            long sommeCombats = 0, sommeVagues = 0, nbLignes = 0;
            for (TbSyntheseRoundProjection l : lignes) {
                if (l.getRoundNum() == round) {
                    sommeCombats += nz(l.getCombats());
                    sommeVagues += nz(l.getVagues());
                    nbLignes++;
                }
            }
            guilde.add(new GuildeRoundMoyenne(round,
                    nbLignes > 0 ? (double) sommeCombats / nbLignes : 0.0,
                    nbLignes > 0 ? (double) sommeVagues / nbLignes : 0.0));
        }

        Map<String, Long> nbR8ParJoueur = rosterUnitActuelRepository.countUnitesR8PlusParJoueur().stream()
                .collect(Collectors.toMap(RelicR8CountProjection::getPlayerId, RelicR8CountProjection::getNbR8Plus));

        List<Joueur> joueursGuilde = joueurRepository.findAllByPresentInGuildTrue();

        List<JoueurSynthese> joueurs = new ArrayList<>();
        for (Joueur j : joueursGuilde) {
            String playerId = j.getPlayerId();
            Map<Integer, Map<Long, long[]>> parRound = donnees.getOrDefault(playerId, Map.of());

            List<RoundColonne> rounds = new ArrayList<>();
            for (int round : ROUNDS) {
                Map<Long, long[]> parTb = parRound.getOrDefault(round, Map.of());
                List<BtCellule> historique = new ArrayList<>();
                for (Long tbId : tbIdsOrdonnes) {
                    long[] v = parTb.get(tbId);
                    historique.add(new BtCellule(tbId, btEndTimes.get(tbId),
                            v != null ? v[0] : 0L, v != null ? v[1] : 0L));
                }
                rounds.add(new RoundColonne(round, historique));
            }

            Set<Long> btJoueur = btAvecDonneesParJoueur.getOrDefault(playerId, Set.of());
            double totalPoints = 0.0;
            for (Long tbId : btJoueur) {
                for (int round : ROUNDS) {
                    long[] v = parRound.getOrDefault(round, Map.of()).get(tbId);
                    if (v != null) {
                        totalPoints += v[0] * POIDS_VAGUE.get(round) + v[1] * POIDS_COMBAT.get(round);
                    }
                }
            }
            Double note = btJoueur.isEmpty() ? null : totalPoints / btJoueur.size();

            joueurs.add(new JoueurSynthese(
                    playerId,
                    j.getPlayerName(),
                    note,
                    nbR8ParJoueur.getOrDefault(playerId, 0L),
                    rounds));
        }

        // Tri par défaut : note croissante, joueurs sans note en fin de liste
        joueurs.sort((a, b) -> {
            if (a.note() == null && b.note() == null) return a.playerName().compareToIgnoreCase(b.playerName());
            if (a.note() == null) return 1;
            if (b.note() == null) return -1;
            return Double.compare(a.note(), b.note());
        });

        List<ChartPoint> pointsGraphique = joueurs.stream()
                .filter(j -> j.note() != null)
                .map(j -> new ChartPoint(j.playerName(), j.note(), j.nbR8Plus()))
                .toList();

        return new SyntheseResult(guilde, joueurs, pointsGraphique);
    }

    private long nz(Long v) { return v != null ? v : 0L; }
}