package swgohManager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.MatriceRelicResultatDTO;
import swgohManager.controller.dto.MatriceResultatDTO;
import swgohManager.controller.dto.OmicronOptionProjection;
import swgohManager.controller.dto.OmicronUnitDTO;
import swgohManager.controller.dto.RelicJoueurProjection;
import swgohManager.repository.OmicronJoueurProjection;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;

@Service
@RequiredArgsConstructor
public class AnalyseRosterService {

    private final RosterUnitSkillActuelRepository repository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository; // <-- Injection

    public List<OmicronUnitDTO> getReferentielOmicrons() {
        List<OmicronOptionProjection> options = repository.findOptionsOmicron();

        Map<String, List<OmicronOptionProjection>> parUnite = options.stream()
                .collect(Collectors.groupingBy(
                        OmicronOptionProjection::getBaseId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<OmicronUnitDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<OmicronOptionProjection>> entry : parUnite.entrySet()) {
            String baseId = entry.getKey();
            List<OmicronOptionProjection> listOptions = entry.getValue();

            String unitName = listOptions.get(0).getLibelle();

            List<OmicronUnitDTO.SkillDTO> skills = listOptions.stream()
                    .map(o -> OmicronUnitDTO.SkillDTO.builder()
                            .idSkill(o.getIdSkill())
                            .skillType(o.getType())
                            .numero(o.getNumero())
                            .build())
                    .distinct()
                    .collect(Collectors.toList());

            result.add(OmicronUnitDTO.builder()
                    .baseId(baseId)
                    .unitName(unitName)
                    .skills(skills)
                    .build());
        }

        return result;
    }

    public List<MatriceResultatDTO> calculerMatrice(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<OmicronJoueurProjection> omicronsJoueurs = repository.findOmicronsAppliquesPourSkills(skillIds);

        Map<String, List<OmicronJoueurProjection>> parJoueur = omicronsJoueurs.stream()
                .collect(Collectors.groupingBy(OmicronJoueurProjection::getPlayerId));

        List<MatriceResultatDTO> resultats = new ArrayList<>();

        for (Map.Entry<String, List<OmicronJoueurProjection>> entry : parJoueur.entrySet()) {
            List<OmicronJoueurProjection> listSkills = entry.getValue();

            // 1. Extraction du vrai nom du joueur depuis la projection (avec fallback sur l'ID)
            String rawName = listSkills.get(0).getPlayerName();
            String playerName = (rawName != null && !rawName.isBlank()) ? rawName : entry.getKey();

            Map<String, Boolean> omicronsPossedes = new HashMap<>();
            for (String skillId : skillIds) {
                omicronsPossedes.put(skillId, false);
            }

            long nbActifs = 0;
            for (OmicronJoueurProjection oj : listSkills) {
                boolean isApplied = Boolean.TRUE.equals(oj.getIsApplied());
                omicronsPossedes.put(oj.getIdSkill(), isApplied);
                if (isApplied) {
                    nbActifs++;
                }
            }

            double pourcentage = ((double) nbActifs / skillIds.size()) * 100.0;

            // 2. Passage de playerName (et non entry.getKey()) dans le DTO
            resultats.add(new MatriceResultatDTO(playerName, pourcentage, omicronsPossedes));
        }

        resultats.sort(Comparator.comparing(MatriceResultatDTO::pourcentageActif).reversed()
                .thenComparing(MatriceResultatDTO::playerName));

        return resultats;
    }
    
    public List<MatriceRelicResultatDTO> calculerMatriceRelics(List<String> baseIds, List<Integer> targetRelics) {
        if (baseIds == null || baseIds.isEmpty() || targetRelics == null) {
            return Collections.emptyList();
        }

        // 1. Filtrer et coupler baseId avec son objectif de rélique
        Map<String, Integer> objectifsMap = new LinkedHashMap<>();
        for (int i = 0; i < baseIds.size(); i++) {
            String baseId = baseIds.get(i);
            Integer target = (i < targetRelics.size()) ? targetRelics.get(i) : null;
            if (baseId != null && !baseId.isBlank() && target != null) {
                objectifsMap.put(baseId, target);
            }
        }

        if (objectifsMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> validBaseIds = new ArrayList<>(objectifsMap.keySet());
        List<RelicJoueurProjection> mecaRelics = rosterUnitActuelRepository.findRelicsPourUnites(validBaseIds);

        Map<String, List<RelicJoueurProjection>> parJoueur = mecaRelics.stream()
                .collect(Collectors.groupingBy(RelicJoueurProjection::getPlayerId));

        List<MatriceRelicResultatDTO> resultats = new ArrayList<>();

        for (Map.Entry<String, List<RelicJoueurProjection>> entry : parJoueur.entrySet()) {
            List<RelicJoueurProjection> listUnits = entry.getValue();

            String rawName = listUnits.get(0).getPlayerName();
            String playerName = (rawName != null && !rawName.isBlank()) ? rawName : entry.getKey();

            Map<String, Integer> relicsPossedes = new HashMap<>();
            Map<String, Boolean> objectifsAtteints = new HashMap<>();

            for (String baseId : validBaseIds) {
                relicsPossedes.put(baseId, -1);
                objectifsAtteints.put(baseId, false);
            }

            long nbAtteints = 0;
            for (RelicJoueurProjection rj : listUnits) {
                Integer relicActuel = rj.getRelic() != null ? rj.getRelic() : -1;
                relicsPossedes.put(rj.getBaseId(), relicActuel);

                Integer targetRelic = objectifsMap.get(rj.getBaseId());
                boolean isOk = targetRelic != null && relicActuel >= targetRelic;
                objectifsAtteints.put(rj.getBaseId(), isOk);

                if (isOk) {
                    nbAtteints++;
                }
            }

            double pourcentage = ((double) nbAtteints / validBaseIds.size()) * 100.0;
            resultats.add(new MatriceRelicResultatDTO(playerName, pourcentage, relicsPossedes, objectifsAtteints));
        }

        resultats.sort(Comparator.comparing(MatriceRelicResultatDTO::pourcentageActif).reversed()
                .thenComparing(MatriceRelicResultatDTO::playerName));

        return resultats;
    }
}