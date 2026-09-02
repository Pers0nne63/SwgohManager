package swgohManager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swgohManager.model.PlanFarmDatacron;
import swgohManager.model.PlanFarmDatacronMecanique;
import swgohManager.model.PlanFarmDatacronStat;
import swgohManager.repository.PlanFarmDatacronMecaniqueRepository;
import swgohManager.repository.PlanFarmDatacronRepository;
import swgohManager.repository.PlanFarmDatacronStatRepository;

@Service
@RequiredArgsConstructor
public class PlanFarmDatacronService {

    private final PlanFarmDatacronRepository planFarmDatacronRepository;
    private final PlanFarmDatacronMecaniqueRepository mecaniqueRepository;
    private final PlanFarmDatacronStatRepository statRepository;
    private final PlanFarmDatacronOptionsService optionsService;

    public record MecaniqueAffichee(Long id, Integer tier, String description) {}
    public record StatAffichee(Long id, String statLibelle, BigDecimal value) {}
    public record DatacronCarte(Long id, String nom, List<MecaniqueAffichee> mecaniques, List<StatAffichee> stats) {}
    public record SetCartes(String setId, List<DatacronCarte> datacrons) {}

    @Transactional
    public void creer(String setId,
                       String nom,
                       List<String> mecaniquesBrutes,
                       List<String> statsSelectionnees,
                       Map<String, String> valeursBrutes) {

        PlanFarmDatacron datacron = planFarmDatacronRepository.save(PlanFarmDatacron.builder()
                .setId(setId)
                .nom(nom)
                .dateCreation(LocalDateTime.now())
                .build());

        if (mecaniquesBrutes != null) {
            for (String brut : mecaniquesBrutes) {
                String[] parts = brut.split("\\|", 2);
                if (parts.length != 2) continue;
                mecaniqueRepository.save(PlanFarmDatacronMecanique.builder()
                        .planFarmDatacronId(datacron.getId())
                        .tier(Integer.valueOf(parts[0]))
                        .abilityId(parts[1])
                        .build());
            }
        }

        if (statsSelectionnees != null) {
            for (String statType : statsSelectionnees) {
                String brut = valeursBrutes.get("statValue_" + statType);
                BigDecimal valeur = (brut != null && !brut.isBlank())
                        ? new BigDecimal(brut.replace(",", "."))
                        : null;

                statRepository.save(PlanFarmDatacronStat.builder()
                        .planFarmDatacronId(datacron.getId())
                        .statType(statType)
                        .statValue(valeur)
                        .build());
            }
        }
    }

    @Transactional
    public void supprimerMecanique(Long id) {
        mecaniqueRepository.deleteById(id);
    }

    @Transactional
    public void supprimerStat(Long id) {
        statRepository.deleteById(id);
    }

    @Transactional
    public void supprimerDatacron(Long id) {
        mecaniqueRepository.deleteByPlanFarmDatacronId(id);
        statRepository.deleteByPlanFarmDatacronId(id);
        planFarmDatacronRepository.deleteById(id);
    }

    @Transactional
    public void supprimerParSet(String setId) {
        List<Long> ids = planFarmDatacronRepository.findBySetId(setId).stream()
                .map(PlanFarmDatacron::getId)
                .toList();
        for (Long id : ids) {
            mecaniqueRepository.deleteByPlanFarmDatacronId(id);
            statRepository.deleteByPlanFarmDatacronId(id);
        }
        planFarmDatacronRepository.deleteBySetId(setId);
    }

    public List<SetCartes> listerParSet() {
        List<PlanFarmDatacronOptionsService.SetOptions> options = optionsService.construire();

        Map<String, String> descriptionParMecanique = new HashMap<>();
        Map<String, String> libelleParStat = new HashMap<>();
        for (var setOption : options) {
            for (var m : setOption.mecaniques()) {
                descriptionParMecanique.putIfAbsent(m.tier() + "|" + m.abilityId(), m.descriptionComplete());
            }
            for (var s : setOption.stats()) {
                libelleParStat.putIfAbsent(s.statType(), s.statLibelle());
            }
        }

        List<PlanFarmDatacron> datacrons = planFarmDatacronRepository.findAll();
        Map<String, List<PlanFarmDatacron>> parSet = datacrons.stream()
                .collect(Collectors.groupingBy(PlanFarmDatacron::getSetId, LinkedHashMap::new, Collectors.toList()));

        List<SetCartes> resultat = new ArrayList<>();

        parSet.entrySet().stream()
                .sorted(Map.Entry.<String, List<PlanFarmDatacron>>comparingByKey().reversed())
                .forEach(entry -> {
                    List<DatacronCarte> cartes = entry.getValue().stream()
                            .map(d -> {
                                List<MecaniqueAffichee> mecaniques = mecaniqueRepository.findByPlanFarmDatacronId(d.getId()).stream()
                                        .sorted(Comparator.comparing(PlanFarmDatacronMecanique::getTier))
                                        .map(m -> new MecaniqueAffichee(
                                                m.getId(),
                                                m.getTier(),
                                                descriptionParMecanique.getOrDefault(m.getTier() + "|" + m.getAbilityId(), m.getAbilityId())
                                        ))
                                        .toList();

                                List<StatAffichee> stats = statRepository.findByPlanFarmDatacronId(d.getId()).stream()
                                        .map(s -> new StatAffichee(
                                                s.getId(),
                                                libelleParStat.getOrDefault(s.getStatType(), s.getStatType()),
                                                s.getStatValue()
                                        ))
                                        .toList();
                                String nomAffiche = (d.getNom() != null && !d.getNom().isBlank()) ? d.getNom() : "Datacron #" + d.getId();
                                return new DatacronCarte(d.getId(), nomAffiche, mecaniques, stats);
                            })
                            .toList();

                    resultat.add(new SetCartes(entry.getKey(), cartes));
                });

        return resultat;
    }
}