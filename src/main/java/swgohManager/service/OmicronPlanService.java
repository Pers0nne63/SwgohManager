package swgohManager.service;

import swgohManager.controller.dto.OmicronOptionProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OmicronPlanService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;

    public record Option(String label, String baseId, String idSkill) {}

    public List<OmicronPlan> getAll() {
        return omicronPlanRepository.findAllByOrderByPrioriteAscBaseIdAsc();
    }

    public List<Option> getOptionsDisponibles() {
        return rosterUnitSkillActuelRepository.findOptionsOmicron().stream()
                .map(this::construireOption)
                .toList();
    }

    public Map<String, Option> getOptionsParLabel() {
        Map<String, Option> map = new LinkedHashMap<>();
        for (Option o : getOptionsDisponibles()) {
            map.put(o.label(), o);
        }
        return map;
    }

    private Option construireOption(OmicronOptionProjection p) {
        String suffixe = p.getType() != null ? p.getType().toUpperCase() : "?";
        if (p.getNumero() != null) {
            suffixe += String.format("%02d", p.getNumero());
        }
        String label = p.getBaseId() + " - " + suffixe;
        return new Option(label, p.getBaseId(), p.getIdSkill());
    }

    @Transactional
    public void ajouter(String label, Integer priorite) {
        Option option = getOptionsParLabel().get(label);
        if (option == null) {
            throw new IllegalArgumentException("Sélection invalide : " + label);
        }
        OmicronPlan plan = OmicronPlan.builder()
                .baseId(option.baseId())
                .idSkill(option.idSkill())
                .priorite(priorite)
                .build();
        omicronPlanRepository.save(plan);
    }

    @Transactional
    public void modifier(Long id, Integer priorite) {
        omicronPlanRepository.findById(id).ifPresent(plan -> {
            plan.setPriorite(priorite);
            omicronPlanRepository.save(plan);
        });
    }

    @Transactional
    public void supprimer(Long id) {
        omicronPlanRepository.deleteById(id);
    }
}