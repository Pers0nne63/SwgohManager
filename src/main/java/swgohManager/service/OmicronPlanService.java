package swgohManager.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.OmicronOptionProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import swgohManager.controller.dto.OmicronPlanDto;
import java.util.stream.Collectors;

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
                .filter(p -> p.getLibelle() != null && !p.getLibelle().startsWith("UNIT_"))
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
        String label = p.getLibelle() + " - " + suffixe;
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
    
    public List<OmicronPlanDto> getAllEnrichis() {
        // Map d'options existantes (clé: baseId_idSkill -> valeur: Option)
        Map<String, Option> optionsMap = getOptionsDisponibles().stream()
                .collect(Collectors.toMap(
                        o -> o.baseId() + "_" + o.idSkill(),
                        o -> o,
                        (v1, v2) -> v1
                ));

        return omicronPlanRepository.findAllByOrderByPrioriteAscBaseIdAsc().stream()
                .map(p -> {
                    String key = p.getBaseId() + "_" + p.getIdSkill();
                    Option option = optionsMap.get(key);

                    String nomUnite = p.getBaseId();
                    String nomSkill = p.getIdSkill();

                    if (option != null) {
                        // découpe le label "Nom Unité - TYPE01"
                        String[] parts = option.label().split(" - ", 2);
                        nomUnite = parts[0];
                        if (parts.length > 1) {
                            nomSkill = parts[1];
                        }
                    }

                    return OmicronPlanDto.builder()
                            .id(p.getId())
                            .baseId(p.getBaseId())
                            .nomUnite(nomUnite)
                            .idSkill(p.getIdSkill())
                            .nomSkill(nomSkill)
                            .priorite(p.getPriorite())
                            .build();
                })
                .toList();
    }
}