package swgohManager.service;

import swgohManager.controller.dto.TbRoundStatsProjection;
import swgohManager.repository.TbScoreJoueurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TbStatsService {

    private final TbScoreJoueurRepository tbScoreJoueurRepository;

    public record TbSummaryRow(
            LocalDateTime endTime, Integer totalStars,
            long vaguesP3, long vaguesP4, long vaguesP5, long vaguesP6,
            long msTenteesTotal,
            String starsTrend, String p3Trend, String p4Trend, String p5Trend, String p6Trend, String msTrend
    ) {}

    private record Brut(LocalDateTime endTime, Integer totalStars, long p3, long p4, long p5, long p6, long msTotal) {}

    public List<TbSummaryRow> getSyntheseTb(String playerId) {
        List<TbRoundStatsProjection> lignes = tbScoreJoueurRepository.findTbRoundStats(playerId);

        Map<Long, List<TbRoundStatsProjection>> parTb = lignes.stream()
                .collect(Collectors.groupingBy(TbRoundStatsProjection::getTerritoryBattleId, LinkedHashMap::new, Collectors.toList()));

        List<Long> idsTriesAsc = parTb.keySet().stream()
                .sorted(Comparator.comparing(id -> parTb.get(id).get(0).getEndTime()))
                .toList();

        List<Brut> bruts = new ArrayList<>();
        for (Long tbId : idsTriesAsc) {
            List<TbRoundStatsProjection> rounds = parTb.get(tbId);
            LocalDateTime endTime = rounds.get(0).getEndTime();
            Integer totalStars = rounds.get(0).getTotalStars();
            long p3 = 0, p4 = 0, p5 = 0, p6 = 0, msTotal = 0;

            for (TbRoundStatsProjection r : rounds) {
                long vagues = r.getVagues() != null ? r.getVagues() : 0L;
                long ms = r.getMsTentees() != null ? r.getMsTentees() : 0L;
                msTotal += ms;
                if (r.getRoundNum() != null) {
                    switch (r.getRoundNum()) {
                        case 3 -> p3 = vagues;
                        case 4 -> p4 = vagues;
                        case 5 -> p5 = vagues;
                        case 6 -> p6 = vagues;
                        default -> {}
                    }
                }
            }
            bruts.add(new Brut(endTime, totalStars, p3, p4, p5, p6, msTotal));
        }

        List<TbSummaryRow> resultat = new ArrayList<>();
        for (int i = 0; i < bruts.size(); i++) {
            Brut c = bruts.get(i);
            Brut precedent = i > 0 ? bruts.get(i - 1) : null;

            resultat.add(new TbSummaryRow(
                    c.endTime(), c.totalStars(), c.p3(), c.p4(), c.p5(), c.p6(), c.msTotal(),
                    tendance(c.totalStars(), precedent != null ? precedent.totalStars() : null),
                    tendance(c.p3(), precedent != null ? precedent.p3() : null),
                    tendance(c.p4(), precedent != null ? precedent.p4() : null),
                    tendance(c.p5(), precedent != null ? precedent.p5() : null),
                    tendance(c.p6(), precedent != null ? precedent.p6() : null),
                    tendance(c.msTotal(), precedent != null ? precedent.msTotal() : null)
            ));
        }

        Collections.reverse(resultat); // le plus récent en premier
        return resultat;
    }

    private String tendance(Number actuel, Number precedent) {
        if (actuel == null || precedent == null) return null;
        double a = actuel.doubleValue(), p = precedent.doubleValue();
        if (a > p) return "up";
        if (a < p) return "down";
        return "same";
    }
}