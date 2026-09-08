package swgohManager.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.Joueur;
import swgohManager.model.PlayerModQActuel;
import swgohManager.model.PlayerModQHistorique;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerModQHistoriqueRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerModQService {

    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final PlayerModQHistoriqueRepository playerModQHistoriqueRepository;
    private final JoueurRepository joueurRepository;
    private final ModQCalculationService modQCalculationService; // NOUVELLE dépendance

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculerEtEnregistrer(String playerId, List<RosterUnitModActuel> modsActuels, Long idSync) {

        List<ModQCalculationService.ModSecondaryValue> valeurs = modsActuels.stream()
                .map(m -> new ModQCalculationService.ModSecondaryValue(m.getIdSecondaire(), m.getValeurSecondaire()))
                .collect(Collectors.toList());

        ModQCalculationService.ModSpeedCounts counts = modQCalculationService.calculerRepartitionVitesse(valeurs);

        Long gpChar = joueurRepository.findByPlayerId(playerId)
                .map(Joueur::getCharacterGalacticPower)
                .orElse(null);

        Double modQ = modQCalculationService.calculerModQ(counts, gpChar);

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
        existant.setMod25Plus(counts.mod25Plus());
        existant.setMod20_24(counts.mod20_24());
        existant.setMod15_19(counts.mod15_19());
        existant.setMod10_14(counts.mod10_14());
        existant.setModQ(modQ);
        existant.setIdSync(idSync);
        playerModQActuelRepository.save(existant);

        log.info("ModQ calculé pour {} : {} (25+={}, 20-24={}, 15-19={}, 10-14={})",
                playerId, modQ, counts.mod25Plus(), counts.mod20_24(), counts.mod15_19(), counts.mod10_14());
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            playerModQActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            playerModQActuelRepository.flush();
        }
    }
}