package swgohManager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.BaseIdLibelleProjection;
import swgohManager.controller.dto.PlayerDatacronProjection;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;
import swgohManager.repository.UnitDefinitionRepository;

@Service
@RequiredArgsConstructor
public class PlayerDatacronViewService {

    private final PlayerDatacronAffixActuelRepository playerDatacronAffixActuelRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record StatBrute(String description, BigDecimal value) {}
    public record TierAffixe(Integer ordre, String description) {}

    public record DatacronCard(
            String idDatacron,
            String titre,
            List<StatBrute> statsBrutes,
            List<TierAffixe> tiers
    ) {}

    public record DatacronSetTab(
            String setId,
            List<DatacronCard> focusDatacrons,   // focused = true
            List<DatacronCard> autresDatacrons   // focused = false, triés par ordre maxi décroissant
    ) {}

    public List<DatacronSetTab> construire(String playerId) {
        List<PlayerDatacronProjection> lignes = playerDatacronAffixActuelRepository.findPlayersDatacron(playerId);

        Map<String, String> libellesParBaseId = unitDefinitionRepository.findDistinctBaseIdsAvecLibelle().stream()
                .collect(Collectors.toMap(
                        p -> p.getBaseId().toUpperCase(),
                        BaseIdLibelleProjection::getLibelle,
                        (a, b) -> a
                ));

        // Regroupement : setId -> idDatacron -> lignes
        Map<String, Map<String, List<PlayerDatacronProjection>>> parSetPuisDatacron = lignes.stream()
                .collect(Collectors.groupingBy(
                        PlayerDatacronProjection::getSetId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(PlayerDatacronProjection::getIdDatacron, LinkedHashMap::new, Collectors.toList())
                ));

        List<DatacronSetTab> resultat = new ArrayList<>();

        parSetPuisDatacron.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(setEntry -> {

                    List<DatacronCard> focusCards = new ArrayList<>();
                    // On garde temporairement l'ordre maxi pour pouvoir trier "autres" ensuite
                    List<Map.Entry<Integer, DatacronCard>> autresAvecOrdreMax = new ArrayList<>();

                    setEntry.getValue().forEach((idDatacron, lignesDatacron) -> {

                        boolean focused = lignesDatacron.stream()
                                .anyMatch(l -> Boolean.TRUE.equals(l.getFocused()));

                        List<StatBrute> statsBrutes = lignesDatacron.stream()
                                .filter(l -> l.getOrdre() == null || l.getOrdre() == 0)
                                .map(l -> new StatBrute(l.getDescription(), l.getValue()))
                                .toList();

                        List<TierAffixe> tiers = lignesDatacron.stream()
                                .filter(l -> l.getOrdre() != null && l.getOrdre() != 0)
                                .sorted(Comparator.comparing(PlayerDatacronProjection::getOrdre))
                                .map(l -> new TierAffixe(l.getOrdre(), l.getDescription()))
                                .toList();

                        if (focused) {
                            String titre = lignesDatacron.stream()
                                    .map(PlayerDatacronProjection::getFocusLibelle)
                                    .filter(fl -> fl != null && !fl.isBlank())
                                    .findFirst()
                                    .map(fl -> libellesParBaseId.getOrDefault(fl.toUpperCase(), fl))
                                    .orElse(idDatacron);

                            focusCards.add(new DatacronCard(idDatacron, titre, statsBrutes, tiers));
                        } else {
                            String titre = calculerTitreNonFocus(idDatacron, lignesDatacron, libellesParBaseId);

                            int ordreMax = lignesDatacron.stream()
                                    .filter(l -> l.getOrdre() != null)
                                    .mapToInt(PlayerDatacronProjection::getOrdre)
                                    .max()
                                    .orElse(0);

                            autresAvecOrdreMax.add(Map.entry(ordreMax, new DatacronCard(idDatacron, titre, statsBrutes, tiers)));
                        }
                    });

                    List<DatacronCard> autresCards = autresAvecOrdreMax.stream()
                            .sorted(Comparator.<Map.Entry<Integer, DatacronCard>>comparingInt(Map.Entry::getKey).reversed())
                            .map(Map.Entry::getValue)
                            .toList();

                    resultat.add(new DatacronSetTab(setEntry.getKey(), focusCards, autresCards));
                });

        return resultat;
    }

    // Titre pour les datacrons NON focus : dernière target connue à l'ordre 9 (seul ordre concerné hors focus)
    private String calculerTitreNonFocus(String idDatacron,
		            List<PlayerDatacronProjection> lignesDatacron,
		            Map<String, String> libellesParBaseId) {
		
		PlayerDatacronProjection derniereLigneAvecTarget = lignesDatacron.stream()
		.filter(l -> l.getOrdre() != null && l.getOrdre() != 0)
		.filter(l -> l.getTarget() != null && !l.getTarget().isBlank())
		.max(Comparator.comparing(PlayerDatacronProjection::getOrdre))
		.orElse(null);
		
		if (derniereLigneAvecTarget == null) {
		return idDatacron;
		}
		
		String target = derniereLigneAvecTarget.getTarget();
		return libellesParBaseId.getOrDefault(target.toUpperCase(), target);
	}
}