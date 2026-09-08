package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.DatacronDTO;
import swgohManager.model.PlayerDatacronActuel;
import swgohManager.model.PlayerDatacronAffixActuel;
import swgohManager.repository.PlayerDatacronActuelRepository;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerDatacronService {

    private final PlayerDatacronActuelRepository datacronRepository;
    private final PlayerDatacronAffixActuelRepository affixRepository;
    private final DatacronSyncCalculationService datacronSyncCalculationService;

    @Transactional
    public void enregistrer(String playerId, PlayerResponse response) {
        datacronRepository.deleteByPlayerId(playerId);
        affixRepository.deleteByPlayerId(playerId);
        datacronRepository.flush();
        affixRepository.flush();

        List<DatacronDTO> datacronsDto = datacronSyncCalculationService.construireDatacronsDto(response.datacron());
        if (datacronsDto.isEmpty()) return;

        List<PlayerDatacronActuel> datacrons = new ArrayList<>();
        List<PlayerDatacronAffixActuel> affixes = new ArrayList<>();

        for (DatacronDTO d : datacronsDto) {
            datacrons.add(PlayerDatacronActuel.builder()
                    .playerId(playerId).idDatacron(d.idDatacron()).setId(d.setId()).templateId(d.templateId())
                    .locked(d.locked()).rerollIndex(d.rerollIndex()).rerollCount(d.rerollCount())
                    .focused(d.focused())
                    .build());

            for (DatacronDTO.AffixDTO a : d.affixes()) {
                affixes.add(PlayerDatacronAffixActuel.builder()
                        .playerId(playerId).idDatacron(d.idDatacron()).ordre(a.ordre())
                        .tag(a.tag()).targetRule(a.targetRule()).abilityId(a.abilityId())
                        .statType(a.statType()).statValue(a.statValue())
                        .requiredUnitTier(a.requiredUnitTier()).requiredRelicTier(a.requiredRelicTier())
                        .scopeIcon(a.scopeIcon())
                        .build());
            }
        }

        datacronRepository.saveAll(datacrons);
        affixRepository.saveAll(affixes);
        log.info("Datacrons enregistrés pour {} : {} datacron(s), {} affix(es)",
                playerId, datacrons.size(), affixes.size());
    }

    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            datacronRepository.deleteByPlayerIdNotIn(joueursActifs);
            datacronRepository.flush();
            affixRepository.deleteByPlayerIdNotIn(joueursActifs);
            affixRepository.flush();
        }
    }
}