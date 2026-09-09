package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalOmicronComparisonService {

    private final OmicronPlanProgressService omicronPlanProgressService;

    public record DetailRow(String baseId, String label, boolean atteint) {}
    public record ExternalOmicronProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}

    public ExternalOmicronProgress comparer(String playerId) {
        OmicronPlanCalculationService.PlayerOmicronProgress progress =
                omicronPlanProgressService.getProgression(playerId, Portee.EXTERNE);

        int totalAtteint = 0;
        int totalTotal = 0;
        List<DetailRow> details = new ArrayList<>();

        for (OmicronPlanCalculationService.PrioriteSummary ps : progress.parPriorite().values()) {
            totalAtteint += ps.atteint();
            totalTotal += ps.total();
            for (OmicronPlanCalculationService.DetailRow d : ps.details()) {
                details.add(new DetailRow(d.baseId(), d.label(), d.atteint()));
            }
        }

        Double pourcentage = totalTotal > 0 ? (100.0 * totalAtteint / totalTotal) : null;
        return new ExternalOmicronProgress(totalAtteint, totalTotal, pourcentage, details);
    }
}