package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.PlayerEraUnitStatusActuel;
import swgohManager.repository.PlayerEraUnitStatusActuelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerEraUnitStatusService {

    private final PlayerEraUnitStatusActuelRepository repository;

    @Transactional
    public void enregistrer(String playerId, PlayerResponse response) {
        List<PlayerResponse.EraUnitStatusRaw> statutsBruts = response.eraUnitStatus();

        repository.deleteByPlayerId(playerId);
        repository.flush();

        if (statutsBruts == null || statutsBruts.isEmpty()) {
            return;
        }

        List<PlayerEraUnitStatusActuel> statuts = new ArrayList<>();
        for (PlayerResponse.EraUnitStatusRaw s : statutsBruts) {
            statuts.add(PlayerEraUnitStatusActuel.builder()
                    .playerId(playerId).unitBaseId(s.unitBaseId()).eraLevel(s.eraLevel())
                    .build());
        }

        repository.saveAll(statuts);
        log.info("Statuts d'ère enregistrés pour {} : {} unité(s)", playerId, statuts.size());
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
        	repository.deleteByPlayerIdNotIn(joueursActifs);
        	repository.flush(); // Force l'exécution immédiate
        }
    }
}