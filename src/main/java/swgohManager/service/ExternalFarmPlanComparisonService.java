package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.repository.ExternalRosterUnitActuelRepository;
import swgohManager.repository.FarmPlanRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalFarmPlanComparisonService {

    private final FarmPlanRepository farmPlanRepository;
    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final FarmPlanCalculationService farmPlanCalculationService;

    public record DetailRow(String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}
    public record ExternalFarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}

    public ExternalFarmProgress comparer(String playerId) {
        FarmPlanCalculationService.FarmProgress p = farmPlanCalculationService.calculer(
                farmPlanRepository.findAll(),
                externalRosterUnitActuelRepository.findMaxEtoilesRelicByBaseId(playerId)
        );

        List<DetailRow> details = p.details().stream()
                .map(d -> new DetailRow(d.baseId(), d.nomUnite(), d.etoilesCible(), d.relicCible(), d.relicActuel(), d.atteint()))
                .toList();

        return new ExternalFarmProgress(p.atteint(), p.total(), p.pourcentage(), details);
    }
}