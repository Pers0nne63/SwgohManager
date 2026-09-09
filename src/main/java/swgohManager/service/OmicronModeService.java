package swgohManager.service;

import swgohManager.controller.dto.OmicronModeSummaryProjection;
import swgohManager.dto.pivot.UnitSkillDTO;
import swgohManager.model.PlayerOmicronModeActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.PlayerOmicronModeActuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OmicronModeService {

    private static final Set<String> MODES_SUIVIS = Set.of(
            "TERRITORY_BATTLE_BOTH_OMICRON",
            "TERRITORY_WAR_OMICRON",
            "TERRITORY_TOURNAMENT_OMICRON",
            "TERRITORY_TOURNAMENT_3_OMICRON",
            "TERRITORY_TOURNAMENT_5_OMICRON"
    );

    private final PlayerOmicronModeActuelRepository playerOmicronModeActuelRepository;

    @Transactional
    public void calculerEtEnregistrer(String playerId, List<UnitSkillDTO> skillsDto,
                                       Map<String, SkillDefinition> definitions, Long idSync) {
        Map<String, Integer> comptageParMode = new HashMap<>();
        for (UnitSkillDTO skill : skillsDto) {
            if (!Boolean.TRUE.equals(skill.omicronApplied())) continue;
            SkillDefinition def = definitions.get(skill.idSkill());
            if (def == null || def.getOmicronMode() == null) continue;
            if (!MODES_SUIVIS.contains(def.getOmicronMode())) continue;
            comptageParMode.merge(def.getOmicronMode(), 1, Integer::sum);
        }

        playerOmicronModeActuelRepository.deleteByPlayerId(playerId);
        playerOmicronModeActuelRepository.flush();

        List<PlayerOmicronModeActuel> aSauver = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : comptageParMode.entrySet()) {
            aSauver.add(PlayerOmicronModeActuel.builder()
                    .playerId(playerId)
                    .omicronMode(entry.getKey())
                    .nbOmicron(entry.getValue())
                    .idSync(idSync)
                    .build());
        }
        playerOmicronModeActuelRepository.saveAll(aSauver);
    }

    public Map<String, Long> getSyntheseGuilde() {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (String mode : MODES_SUIVIS) {
            resultat.put(mode, 0L);
        }
        for (OmicronModeSummaryProjection p : playerOmicronModeActuelRepository.sommeParMode()) {
            if (MODES_SUIVIS.contains(p.getOmicronMode())) {
                resultat.put(p.getOmicronMode(), p.getTotal());
            }
        }
        return resultat;
    }
}