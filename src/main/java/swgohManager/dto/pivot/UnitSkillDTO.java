package swgohManager.dto.pivot;

import lombok.Builder;

/**
 * DTO pivot représentant une compétence de unit après calcul zeta/omicron,
 * indépendant de l'origine du joueur (interne ou externe) et de la persistance (idSync ou non).
 */
@Builder
public record UnitSkillDTO(
        String idUnit,
        String idSkill,
        Integer tier,
        String type,
        Integer numero,
        Boolean skillZeta,
        Boolean zetaApplied,
        Boolean skillOmicron,
        Boolean omicronApplied
) {}