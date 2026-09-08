package swgohManager.service;

import org.springframework.stereotype.Service;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.DatacronDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Construction commune des datacrons + affixes à partir de la réponse Comlink,
 * utilisée par la synchro interne (PlayerDatacronService) et externe (ExternalPlayerSyncDataService).
 */
@Service
public class DatacronSyncCalculationService {

    public List<DatacronDTO> construireDatacronsDto(List<PlayerResponse.DatacronRaw> datacronsBruts) {
        List<DatacronDTO> resultat = new ArrayList<>();
        if (datacronsBruts == null) return resultat;

        for (PlayerResponse.DatacronRaw d : datacronsBruts) {
            List<DatacronDTO.AffixDTO> affixes = new ArrayList<>();

            if (d.affix() != null) {
                int ordre = 1;
                for (PlayerResponse.AffixRaw a : d.affix()) {
                    affixes.add(DatacronDTO.AffixDTO.builder()
                            .ordre(ordre)
                            .tag(a.tag() != null ? String.join(",", a.tag()) : null)
                            .targetRule(a.targetRule())
                            .abilityId(a.abilityId())
                            .statType(a.statType())
                            .statValue(parseLong(a.statValue()))
                            .requiredUnitTier(a.requiredUnitTier())
                            .requiredRelicTier(a.requiredRelicTier())
                            .scopeIcon(a.scopeIcon())
                            .build());
                    ordre++;
                }
            }

            resultat.add(DatacronDTO.builder()
                    .idDatacron(d.id())
                    .setId(d.setId())
                    .templateId(d.templateId())
                    .locked(d.locked())
                    .rerollIndex(d.rerollIndex())
                    .rerollCount(d.rerollCount())
                    .focused(d.focused())
                    .affixes(affixes)
                    .build());
        }

        return resultat;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }
}