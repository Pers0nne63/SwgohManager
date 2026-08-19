package swgohManager.service;

import swgohManager.model.FarmPlan;
import swgohManager.repository.FarmPlanRepository;
import swgohManager.repository.UnitDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmPlanService {

    private final FarmPlanRepository farmPlanRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public List<FarmPlan> getAll() {
        return farmPlanRepository.findAllByOrderByBaseIdAsc();
    }

    public List<String> getBaseIdsDisponibles() {
        return unitDefinitionRepository.findDistinctBaseIds();
    }

    @Transactional
    public void ajouter(String baseId, Integer etoiles, Integer relic, String tag) {
        FarmPlan plan = FarmPlan.builder()
                .baseId(baseId)
                .etoilesCible(etoiles != null ? etoiles : 7)
                .relicCible(relic != null ? relic : 6)
                .tag(tag)
                .build();
        farmPlanRepository.save(plan);
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