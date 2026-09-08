package swgohManager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.UnitModDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Construction commune des lignes de mods à partir d'un EquippedStatMod Comlink.
 * Utilisé à la fois par les synchros internes (RosterUnitService) et externes (ExternalPlayerSyncService).
 */
@Service
@Slf4j
public class UnitModCalculationService {

    /**
     * Construit une ligne de DTO pivot par stat secondaire du mod (comme l'ancien construireLignesMod).
     * Ne dépend ni de playerId, ni de idSync, ni de l'entité de destination (interne/externe).
     */
    public List<UnitModDTO> construireLignesModDto(String idUnit, PlayerResponse.EquippedStatMod mod) {
        List<UnitModDTO> lignes = new ArrayList<>();

        String definitionId = mod.definitionId();
        String set = definitionId != null && definitionId.length() >= 1 ? definitionId.substring(0, 1) : null;
        String rarity = definitionId != null && definitionId.length() >= 2 ? definitionId.substring(1, 2) : null;
        String position = definitionId != null && definitionId.length() >= 3 ? definitionId.substring(2, 3) : null;

        Integer idPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? mod.primaryStat().stat().unitStatId() : null;
        Long valeurPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? parseLong(mod.primaryStat().stat().unscaledDecimalValue()) : null;

        List<PlayerResponse.SecondaryStat> secondaires = mod.secondaryStat() != null
                ? mod.secondaryStat() : List.of();

        int ordre = 1;
        for (PlayerResponse.SecondaryStat secondaryStat : secondaires) {
            Integer idSecondaire = secondaryStat.stat() != null ? secondaryStat.stat().unitStatId() : null;
            Long valeurSecondaire = secondaryStat.stat() != null
                    ? parseLong(secondaryStat.stat().unscaledDecimalValue()) : null;

            lignes.add(UnitModDTO.builder()
                    .idUnit(idUnit)
                    .idMod(mod.id())
                    .definitionId(definitionId)
                    .set(set)
                    .rarity(rarity)
                    .position(position)
                    .niveau(mod.level())
                    .idPrimaire(idPrimaire)
                    .valeurPrimaire(valeurPrimaire)
                    .idSecondaire(idSecondaire)
                    .valeurSecondaire(valeurSecondaire)
                    .ordreSecondaire(ordre)
                    .build());

            ordre++;
        }

        return lignes;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique du mod : {}", value);
            return null;
        }
    }
}