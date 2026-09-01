package swgohManager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.DatacronAffixOptionProjection;
import swgohManager.repository.DatacronAffixTemplateRepository;

@Service
@RequiredArgsConstructor
public class PlanFarmDatacronOptionsService {

    private final DatacronAffixTemplateRepository datacronAffixTemplateRepository;

    private static final int LONGUEUR_MAX_DESCRIPTION = 50;

    public record MecaniqueOption(Integer tier, String abilityId, String descriptionCourte, String descriptionComplete) {}
    public record StatOption(String statType, String statLibelle) {}
    public record SetOptions(String setId, List<MecaniqueOption> mecaniques, List<StatOption> stats) {}

    public List<SetOptions> construire() {
        List<DatacronAffixOptionProjection> lignes = datacronAffixTemplateRepository.findOptionsFarmPlan();

        Map<String, List<DatacronAffixOptionProjection>> parSet = lignes.stream()
                .filter(l -> l.getSet() != null)
                .collect(Collectors.groupingBy(DatacronAffixOptionProjection::getSet, LinkedHashMap::new, Collectors.toList()));

        List<SetOptions> resultat = new ArrayList<>();

        parSet.entrySet().stream()
                .sorted(Map.Entry.<String, List<DatacronAffixOptionProjection>>comparingByKey().reversed())
                .forEach(entry -> {

                    Map<String, MecaniqueOption> mecaniques = new LinkedHashMap<>();
                    Map<String, StatOption> stats = new LinkedHashMap<>();

                    for (DatacronAffixOptionProjection l : entry.getValue()) {
                        if (l.getTier() != null && l.getAbilityId() != null) {
                            String cle = l.getTier() + "|" + l.getAbilityId();
                            mecaniques.putIfAbsent(cle, new MecaniqueOption(
                                    l.getTier(),
                                    l.getAbilityId(),
                                    tronquer(l.getDescription(), LONGUEUR_MAX_DESCRIPTION),
                                    l.getDescription()
                            ));
                        }
                        if (l.getStatType() != null) {
                            stats.putIfAbsent(l.getStatType(), new StatOption(l.getStatType(), l.getStatLibelle()));
                        }
                    }

                    List<MecaniqueOption> mecaniquesTriees = mecaniques.values().stream()
                            .sorted(Comparator.comparing(MecaniqueOption::tier).thenComparing(MecaniqueOption::abilityId))
                            .toList();

                    List<StatOption> statsTriees = stats.values().stream()
                            .sorted(Comparator.comparing(so -> so.statLibelle() != null ? so.statLibelle() : ""))
                            .toList();

                    resultat.add(new SetOptions(entry.getKey(), mecaniquesTriees, statsTriees));
                });

        return resultat;
    }

    private String tronquer(String texte, int max) {
        if (texte == null) return "";
        return texte.length() <= max ? texte : texte.substring(0, max) + "...";
    }
}