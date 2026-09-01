package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.PlayerDatacronActuel;
import swgohManager.model.PlayerDatacronAffixActuel;
import swgohManager.repository.PlayerDatacronActuelRepository;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerDatacronService {

    private final PlayerDatacronActuelRepository datacronRepository;
    private final PlayerDatacronAffixActuelRepository affixRepository;

    @Transactional
    public void enregistrer(String playerId, PlayerResponse response) {
        List<PlayerResponse.DatacronRaw> datacronsBruts = response.datacron();

        datacronRepository.deleteByPlayerId(playerId);
        affixRepository.deleteByPlayerId(playerId);
        datacronRepository.flush();
        affixRepository.flush();

        if (datacronsBruts == null || datacronsBruts.isEmpty()) {
            return;
        }

        List<PlayerDatacronActuel> datacrons = new ArrayList<>();
        List<PlayerDatacronAffixActuel> affixes = new ArrayList<>();

        for (PlayerResponse.DatacronRaw d : datacronsBruts) {
            datacrons.add(PlayerDatacronActuel.builder()
                    .playerId(playerId).idDatacron(d.id()).setId(d.setId()).templateId(d.templateId())
                    .locked(d.locked()).rerollIndex(d.rerollIndex()).rerollCount(d.rerollCount())
                    .focused(d.focused())
                    .build());

            if (d.affix() != null) {
                int ordre = 1;
                for (PlayerResponse.AffixRaw a : d.affix()) {
                    affixes.add(PlayerDatacronAffixActuel.builder()
                            .playerId(playerId).idDatacron(d.id()).ordre(ordre)
                            .tag(a.tag() != null ? String.join(",", a.tag()) : null)
                            .targetRule(a.targetRule()).abilityId(a.abilityId())
                            .statType(a.statType()).statValue(parseLong(a.statValue()))
                            .requiredUnitTier(a.requiredUnitTier()).requiredRelicTier(a.requiredRelicTier())
                            .scopeIcon(a.scopeIcon())
                            .build());
                    ordre++;
                }
            }
        }

        datacronRepository.saveAll(datacrons);
        affixRepository.saveAll(affixes);

        log.info("Datacrons enregistrés pour {} : {} datacron(s), {} affix(es)",
                playerId, datacrons.size(), affixes.size());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
        	datacronRepository.deleteByPlayerIdNotIn(joueursActifs);
        	datacronRepository.flush(); // Force l'exécution immédiate
        	
        	affixRepository.deleteByPlayerIdNotIn(joueursActifs);
        	affixRepository.flush(); // Force l'exécution immédiate
        }
    }
}