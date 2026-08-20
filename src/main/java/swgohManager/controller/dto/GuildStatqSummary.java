package swgohManager.controller.dto;

import java.util.List;
import java.util.Objects;

public record GuildStatqSummary(
        Double min,
        Double moyenne,
        Double mediane,
        Double max
) {
    public static GuildStatqSummary fromValues(List<Double> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return new GuildStatqSummary(0.0, 0.0, 0.0, 0.0);
        }

        List<Double> sorted = rawValues.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (sorted.isEmpty()) {
            return new GuildStatqSummary(0.0, 0.0, 0.0, 0.0);
        }

        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);
        double moy = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double mediane;
        int size = sorted.size();
        if (size % 2 == 1) {
            mediane = sorted.get(size / 2);
        } else {
            mediane = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        }

        return new GuildStatqSummary(min, moy, mediane, max);
    }
}