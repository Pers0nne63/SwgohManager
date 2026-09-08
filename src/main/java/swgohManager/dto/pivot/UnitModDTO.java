package swgohManager.dto.pivot;

import lombok.Builder;

/**
 * DTO pivot représentant une ligne de mod (une stat secondaire d'un mod équipé),
 * indépendant de l'origine du joueur (interne ou externe) et de la persistance (idSync ou non).
 */
@Builder
public record UnitModDTO(
        String idUnit,
        String idMod,
        String definitionId,
        String set,
        String rarity,
        String position,
        Integer niveau,
        Integer idPrimaire,
        Long valeurPrimaire,
        Integer idSecondaire,
        Long valeurSecondaire,
        Integer ordreSecondaire
) {}