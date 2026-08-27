package swgohManager.service;

import swgohManager.model.TbPlanRound;
import swgohManager.model.TbPlanTemplate;
import swgohManager.model.TbPlaneteReference;
import swgohManager.repository.TbPlanRoundRepository;
import swgohManager.repository.TbPlanTemplateRepository;
import swgohManager.repository.TbPlaneteReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TbPlanService {

    private final TbPlanTemplateRepository tbPlanTemplateRepository;
    private final TbPlanRoundRepository tbPlanRoundRepository;
    private final TbPlaneteReferenceRepository tbPlaneteReferenceRepository;

    public record PlanetOption(Long planeteId, String nom) {}
    public record PlanAvecRounds(TbPlanTemplate plan, List<TbPlanRound> rounds) {}
    public record RoundInput(int roundNum, Long ls, Long ds, Long mix, Long zeffo, Long mandalore) {}

    public List<PlanetOption> getOptionsLs() { return options(1, false); }
    public List<PlanetOption> getOptionsDs() { return options(2, false); }
    public List<PlanetOption> getOptionsMix() { return options(3, false); }

    private List<PlanetOption> options(int conflict, boolean bonus) {
        return tbPlaneteReferenceRepository.findByConflictAndBonus(conflict, bonus).stream()
                .map(p -> new PlanetOption(p.getId(), p.getPlaneteName()))
                .sorted(Comparator.comparing(PlanetOption::nom))
                .toList();
    }

    public Optional<PlanetOption> getZeffo() {
        return tbPlaneteReferenceRepository.findByPlaneteNameIn(List.of("Zeffo")).stream()
                .findFirst().map(p -> new PlanetOption(p.getId(), p.getPlaneteName()));
    }

    public Optional<PlanetOption> getMandalore() {
        return tbPlaneteReferenceRepository.findByPlaneteNameIn(List.of("Mandalore")).stream()
                .findFirst().map(p -> new PlanetOption(p.getId(), p.getPlaneteName()));
    }

    public List<PlanAvecRounds> getAllPlansAvecRounds() {
        return tbPlanTemplateRepository.findAll().stream()
                .map(p -> new PlanAvecRounds(p, tbPlanRoundRepository.findByPlanIdOrderByRoundNumAsc(p.getId())))
                .toList();
    }

    public Map<Long, String> getLibellesPlanetes() {
        return tbPlaneteReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(TbPlaneteReference::getId, TbPlaneteReference::getPlaneteName));
    }

    @Transactional
    public void creerPlan(String nom, Integer etoilesCibles, List<RoundInput> rounds) {
        TbPlanTemplate plan = TbPlanTemplate.builder().nom(nom).etoilesCibles(etoilesCibles).build();
        plan = tbPlanTemplateRepository.save(plan);

        List<TbPlanRound> aSauver = new ArrayList<>();
        for (RoundInput r : rounds) {
            aSauver.add(TbPlanRound.builder()
                    .planId(plan.getId())
                    .roundNum(r.roundNum())
                    .lsPlaneteId(r.ls())
                    .dsPlaneteId(r.ds())
                    .mixPlaneteId(r.mix())
                    .zeffoPlaneteId(r.zeffo())
                    .mandalorePlaneteId(r.mandalore())
                    .build());
        }
        tbPlanRoundRepository.saveAll(aSauver);
    }

    @Transactional
    public void supprimerPlan(Long id) {
        tbPlanRoundRepository.deleteByPlanId(id);
        tbPlanTemplateRepository.deleteById(id);
    }
}