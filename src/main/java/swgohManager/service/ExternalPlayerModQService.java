package swgohManager.service;

import swgohManager.model.ExternalPlayer;
import swgohManager.model.ExternalPlayerModQActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.repository.ExternalPlayerModQActuelRepository;
import swgohManager.repository.ExternalPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerModQService {

    private static final int ID_STAT_SECONDAIRE_VITESSE = 5;
    private static final double DIVISEUR_VALEUR = 100_000_000.0;

    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ExternalPlayerRepository externalPlayerRepository;

    @Transactional
    public void calculerEtEnregistrer(String playerId, List<ExternalRosterUnitModActuel> modsActuels, Long gpChar) {
        int mod25Plus = 0, mod20_24 = 0, mod15_19 = 0, mod10_14 = 0;
        for (ExternalRosterUnitModActuel mod : modsActuels) {
            if (mod.getIdSecondaire() == null || mod.getIdSecondaire() != ID_STAT_SECONDAIRE_VITESSE) continue;
            if (mod.getValeurSecondaire() == null) continue;
            double valeur = mod.getValeurSecondaire() / DIVISEUR_VALEUR;
            if (valeur >= 25) mod25Plus++;
            else if (valeur >= 20) mod20_24++;
            else if (valeur >= 15) mod15_19++;
            else if (valeur >= 10) mod10_14++;
        }

        // Le calcul utilise directement le gpChar transmis
        Double modQ = (gpChar != null && gpChar > 0)
                ? 100_000.0 * (0.8 * mod15_19 + mod20_24 + 1.2 * mod25Plus) / gpChar
                : null;

        externalPlayerModQActuelRepository.deleteByPlayerId(playerId);
        externalPlayerModQActuelRepository.flush();
        
        ExternalPlayerModQActuel entite = ExternalPlayerModQActuel.builder()
                .playerId(playerId)
                .mod25Plus(mod25Plus).mod20_24(mod20_24).mod15_19(mod15_19).mod10_14(mod10_14)
                .modQ(modQ)
                .build();
                
        externalPlayerModQActuelRepository.save(entite);

        log.info("ModQ externe calculé pour {} : {}", playerId, modQ);
    }
}