package swgohManager.service;

import swgohManager.model.Joueur;
import swgohManager.model.PlayerModQActuel;
import swgohManager.model.PlayerModQHistorique;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerModQHistoriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerModQService {

    private static final int ID_STAT_SECONDAIRE_VITESSE = 5; // unitStatId=5 = vitesse
    private static final double DIVISEUR_VALEUR = 100_000_000.0; // même échelle que unscaledDecimalValue

    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final PlayerModQHistoriqueRepository playerModQHistoriqueRepository;
    private final JoueurRepository joueurRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculerEtEnregistrer(String playerId, List<RosterUnitModActuel> modsActuels, Long idSync) {
        int mod25Plus = 0, mod20_24 = 0, mod15_19 = 0, mod10_14 = 0;

        for (RosterUnitModActuel mod : modsActuels) {
            if (mod.getIdSecondaire() == null || mod.getIdSecondaire() != ID_STAT_SECONDAIRE_VITESSE) continue;
            if (mod.getValeurSecondaire() == null) continue;

            double valeur = mod.getValeurSecondaire() / DIVISEUR_VALEUR;
            if (valeur >= 25) mod25Plus++;
            else if (valeur >= 20) mod20_24++;
            else if (valeur >= 15) mod15_19++;
            else if (valeur >= 10) mod10_14++;
        }

        Long gpChar = joueurRepository.findByPlayerId(playerId)
                .map(Joueur::getCharacterGalacticPower)
                .orElse(null);

        Double modQ = (gpChar != null && gpChar > 0)
                ? 100_000.0 * (0.8 * mod15_19 + mod20_24 + 1.2 * mod25Plus) / gpChar
                : null;

        PlayerModQActuel existant = playerModQActuelRepository.findByPlayerId(playerId).orElse(null);

        if (existant != null) {
            playerModQHistoriqueRepository.save(PlayerModQHistorique.builder()
                    .playerId(existant.getPlayerId())
                    .mod25Plus(existant.getMod25Plus())
                    .mod20_24(existant.getMod20_24())
                    .mod15_19(existant.getMod15_19())
                    .mod10_14(existant.getMod10_14())
                    .modQ(existant.getModQ())
                    .idSync(existant.getIdSync())
                    .build());
        } else {
            existant = new PlayerModQActuel();
            existant.setPlayerId(playerId);
        }

        existant.setMod25Plus(mod25Plus);
        existant.setMod20_24(mod20_24);
        existant.setMod15_19(mod15_19);
        existant.setMod10_14(mod10_14);
        existant.setModQ(modQ);
        existant.setIdSync(idSync);

        playerModQActuelRepository.save(existant);

        log.info("ModQ calculé pour {} : {} (25+={}, 20-24={}, 15-19={}, 10-14={})",
                playerId, modQ, mod25Plus, mod20_24, mod15_19, mod10_14);
    }
}