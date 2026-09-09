package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.ExternalPlayerEraUnitStatusActuel;
import swgohManager.model.PlayerEraUnitStatusActuel;
import swgohManager.repository.ExternalPlayerEraUnitStatusActuelRepository;
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
    private final ExternalPlayerEraUnitStatusActuelRepository externalRepository;

    @Transactional
    public void enregistrer(String playerId, PlayerResponse response, Portee portee) {
        List<PlayerResponse.EraUnitStatusRaw> statutsBruts = response.eraUnitStatus();

        if (portee == Portee.GUILDE) {
            repository.deleteByPlayerId(playerId);
            repository.flush();
        } else {
            externalRepository.deleteByPlayerId(playerId);
            externalRepository.flush();
        }

        if (statutsBruts == null || statutsBruts.isEmpty()) {
            return;
        }

        if (portee == Portee.GUILDE) {
            List<PlayerEraUnitStatusActuel> statuts = new ArrayList<>();
            for (PlayerResponse.EraUnitStatusRaw s : statutsBruts) {
                statuts.add(PlayerEraUnitStatusActuel.builder()
                        .playerId(playerId).unitBaseId(s.unitBaseId()).eraLevel(s.eraLevel())
                        .build());
            }
            repository.saveAll(statuts);
            log.info("Statuts d'ère enregistrés pour {} : {} unité(s)", playerId, statuts.size());
        } else {
            List<ExternalPlayerEraUnitStatusActuel> statuts = new ArrayList<>();
            for (PlayerResponse.EraUnitStatusRaw s : statutsBruts) {
                statuts.add(ExternalPlayerEraUnitStatusActuel.builder()
                        .playerId(playerId).unitBaseId(s.unitBaseId()).eraLevel(s.eraLevel())
                        .build());
            }
            externalRepository.saveAll(statuts);
            log.info("Statuts d'ère externes enregistrés pour {} : {} unité(s)", playerId, statuts.size());
        }
    }

    /** Nettoyage propre à la guilde (les joueurs externes sont purgés séparément après 30 jours). */
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            repository.deleteByPlayerIdNotIn(joueursActifs);
            repository.flush();
        }
    }
}