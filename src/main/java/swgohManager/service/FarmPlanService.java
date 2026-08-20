package swgohManager.service;

import swgohManager.controller.dto.FarmPlanDto;
import swgohManager.model.FarmPlan;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.FarmPlanRepository;
import swgohManager.repository.UnitDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmPlanService {

    private final FarmPlanRepository farmPlanRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record UniteOption(String baseId, String libelle) {}

    public List<FarmPlan> getAll() {
        return farmPlanRepository.findAllByOrderByBaseIdAsc();
    }

    /**
     * Retourne les lignes du plan de farm enrichies avec le vrai nom de l'unité.
     */
    public List<FarmPlanDto> getAllEnrichis() {
        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(
                        u -> u.getBaseId().toUpperCase(),
                        UnitDefinition::getLibelle,
                        (v1, v2) -> v1
                ));

        return farmPlanRepository.findAllByOrderByBaseIdAsc().stream()
                .map(p -> FarmPlanDto.builder()
                        .id(p.getId())
                        .baseId(p.getBaseId())
                        .nomUnite(unitMap.getOrDefault(p.getBaseId() != null ? p.getBaseId().toUpperCase() : "", p.getBaseId()))
                        .etoilesCible(p.getEtoilesCible())
                        .relicCible(p.getRelicCible())
                        .tag(p.getTag())
                        .build())
                .toList();
    }

    public List<UniteOption> getUnitesDisponibles() {
        return unitDefinitionRepository.findDistinctBaseIdsAvecLibelle().stream()
                .filter(p -> p.getLibelle() != null && !p.getLibelle().startsWith("UNIT_"))
                .map(p -> new UniteOption(p.getBaseId(), p.getLibelle()))
                .distinct()
                .sorted((a, b) -> a.libelle().compareToIgnoreCase(b.libelle()))
                .toList();
    }

    @Transactional
    public void ajouter(String selection, Integer etoiles, Integer relic, String tag) {
        String baseId = resoudreBaseId(selection);
        if (baseId == null) {
            throw new IllegalArgumentException("Unité non reconnue : " + selection);
        }

        FarmPlan plan = FarmPlan.builder()
                .baseId(baseId)
                .etoilesCible(etoiles != null ? etoiles : 7)
                .relicCible(relic != null ? relic : 6)
                .tag(tag)
                .build();
        farmPlanRepository.save(plan);
    }

    private String resoudreBaseId(String selection) {
        Map<String, String> baseIdParLibelle = new LinkedHashMap<>();
        for (UniteOption o : getUnitesDisponibles()) {
            baseIdParLibelle.put(o.libelle(), o.baseId());
        }
        return baseIdParLibelle.get(selection);
    }

    @Transactional
    public void modifier(Long id, Integer etoiles, Integer relic, String tag) {
        farmPlanRepository.findById(id).ifPresent(plan -> {
            plan.setEtoilesCible(etoiles);
            plan.setRelicCible(relic);
            plan.setTag(tag);
            farmPlanRepository.save(plan);
        });
    }

    @Transactional
    public void supprimer(Long id) {
        farmPlanRepository.deleteById(id);
    }
}