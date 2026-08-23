package swgohManager.service;

import swgohManager.controller.dto.OmicronModeSummaryProjection;
import swgohManager.model.PlayerOmicronModeActuel;
import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.PlayerOmicronModeActuelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OmicronModeService {

    // Seuls ces modes intéressent la synthèse guilde (TB, TW, TT, TT3, TT5)
    private static final Set<String> MODES_SUIVIS = Set.of(
            "TERRITORY_BATTLE_BOTH_OMICRON",
            "TERRITORY_WAR_OMICRON",
            "TERRITORY_TOURNAMENT_OMICRON",
            "TERRITORY_TOURNAMENT_3_OMICRON",
            "TERRITORY_TOURNAMENT_5_OMICRON"
    );

    private final PlayerOmicronModeActuelRepository playerOmicronModeActuelRepository;

    @Transactional
    public void calculerEtEnregistrer(String playerId, List<RosterUnitSkillActuel> skillsActuels,
                                       Map<String, SkillDefinition> definitions, Long idSync) {

        Map<String, Integer> comptageParMode = new HashMap<>();

        for (RosterUnitSkillActuel skill : skillsActuels) {
            if (!Boolean.TRUE.equals(skill.getOmicronApplied())) continue;

            SkillDefinition def = definitions.get(skill.getIdSkill());
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

    /** Lecture pour la carte de la page guilde — somme déjà précalculée, pas de recalcul. */
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