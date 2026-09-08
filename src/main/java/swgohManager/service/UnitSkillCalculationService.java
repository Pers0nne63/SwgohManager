package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.UnitSkillDTO;
import swgohManager.model.SkillDefinition;
import swgohManager.util.SkillIdParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calcul commun des flags zeta/omicron et construction de la liste de compétences pivot.
 * Utilisé à la fois par les synchros internes (RosterUnitService) et externes (ExternalPlayerSyncDataService),
 * ainsi que par le recalcul a posteriori (SkillZetaOmicronService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnitSkillCalculationService {

    /** Résultat du calcul des 4 flags pour une compétence donnée. */
    public record ZetaOmicronFlags(
            Boolean skillZeta,
            Boolean zetaApplied,
            Boolean skillOmicron,
            Boolean omicronApplied
    ) {
        static ZetaOmicronFlags vide() {
            return new ZetaOmicronFlags(null, null, null, null);
        }
    }

    /** Résultat de la construction : la liste de DTO + le nombre de skills sans définition (pour le logging appelant). */
    public record UnitSkillBuildResult(List<UnitSkillDTO> skills, int skillsSansDefinition) {}

    /**
     * Calcule les 4 flags zeta/omicron pour une compétence, à partir de son tier et de sa définition.
     * Formule identique pour zeta et omicron : applied = tier >= (tierRequis - 1).
     */
    public ZetaOmicronFlags calculerFlagsZetaOmicron(Integer tier, SkillDefinition def) {
        if (def == null) {
            return ZetaOmicronFlags.vide();
        }

        Boolean skillZeta = def.getSkillZeta();
        Boolean skillOmicron = def.getSkillOmicron();

        Boolean zetaApplied = Boolean.TRUE.equals(skillZeta)
                && def.getTierZetaRequis() != null
                && tier != null
                && tier >= (def.getTierZetaRequis() - 1);

        Boolean omicronApplied = Boolean.TRUE.equals(skillOmicron)
                && def.getTierOmicronRequis() != null
                && tier != null
                && tier >= (def.getTierOmicronRequis() - 1);

        return new ZetaOmicronFlags(skillZeta, zetaApplied, skillOmicron, omicronApplied);
    }

    /**
     * Construit la liste des compétences (DTO pivot) pour un roster complet, avec calcul zeta/omicron.
     * Ne dépend ni de playerId, ni de idSync, ni de l'entité de destination (interne/externe).
     */
    public UnitSkillBuildResult construireSkillsDto(List<PlayerResponse.RosterUnit> roster,
                                                     Map<String, SkillDefinition> definitions) {
        List<UnitSkillDTO> skillsList = new ArrayList<>();
        int skillsSansDefinition = 0;

        if (roster == null) {
            return new UnitSkillBuildResult(skillsList, skillsSansDefinition);
        }

        for (PlayerResponse.RosterUnit u : roster) {
            if (u.skill() == null) continue;

            for (PlayerResponse.Skill s : u.skill()) {
                SkillIdParser.ParsedSkillId parsed = SkillIdParser.parse(s.id());
                SkillDefinition def = definitions.get(s.id());

                if (def == null) {
                    skillsSansDefinition++;
                }

                ZetaOmicronFlags flags = calculerFlagsZetaOmicron(s.tier(), def);

                skillsList.add(UnitSkillDTO.builder()
                        .idUnit(u.id())
                        .idSkill(s.id())
                        .tier(s.tier())
                        .type(parsed.type())
                        .numero(parsed.numero())
                        .skillZeta(flags.skillZeta())
                        .zetaApplied(flags.zetaApplied())
                        .skillOmicron(flags.skillOmicron())
                        .omicronApplied(flags.omicronApplied())
                        .build());
            }
        }

        return new UnitSkillBuildResult(skillsList, skillsSansDefinition);
    }
}