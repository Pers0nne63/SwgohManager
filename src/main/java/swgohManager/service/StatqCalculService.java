package swgohManager.service;

import swgohManager.controller.dto.RosterIdUnitProjection;
import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.service.StatqCalculationService.StatCible;
import swgohManager.service.StatqCalculationService.StatqResultat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatqCalculService {

    private final JoueurRepository joueurRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;
    private final RosterUnitStatObjectifRepository rosterUnitStatObjectifRepository;
    private final PlayerStatqActuelRepository playerStatqActuelRepository;
    private final PlayerStatqHistoriqueRepository playerStatqHistoriqueRepository;
    private final PlayerStatqDetailActuelRepository playerStatqDetailActuelRepository;
    private final SyncExecutionRepository syncExecutionRepository;
    private final StatqCalculationService statqCalculationService;

    @Transactional
    public String calculerPourTousLesJoueurs() {
        List<StatCible> cibles = statqCalculationService.chargerCibles();

        if (cibles.isEmpty()) {
            log.warn("Aucune configuration dans unit_stat_priority — StatQ non calculé");
            return "Aucune configuration StatQ (unit_stat_priority vide)";
        }

        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();

        Map<String, String> idUnitParPlayerEtBaseId = rosterUnitActuelRepository.findTousLesIdUnitParBaseId().stream()
                .collect(Collectors.toMap(p -> p.getPlayerId() + "|" + p.getBaseId(), RosterIdUnitProjection::getIdUnit, (a, b) -> a));

        Map<String, RosterUnitStatActuel> actuelParUnite = rosterUnitStatActuelRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getPlayerId() + "|" + s.getIdUnit(), s -> s, (a, b) -> a));

        Map<String, RosterUnitStatObjectif> objectifParUnite = rosterUnitStatObjectifRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getPlayerId() + "|" + s.getIdUnit(), s -> s, (a, b) -> a));

        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();

        playerStatqDetailActuelRepository.deleteAll();
        playerStatqDetailActuelRepository.flush();

        List<PlayerStatqDetailActuel> tousLesDetails = new ArrayList<>();
        int joueursCalcules = 0;

        for (Joueur joueur : joueurs) {
            String playerId = joueur.getPlayerId();

            Map<String, String> idUnitParBaseId = new HashMap<>();
            Map<String, UnitStatValues> statsActuel = new HashMap<>();
            Map<String, UnitStatValues> statsObjectif = new HashMap<>();

            for (StatCible cible : cibles) {
                String idUnit = idUnitParPlayerEtBaseId.get(playerId + "|" + cible.baseId());
                if (idUnit == null) continue;
                idUnitParBaseId.put(cible.baseId(), idUnit);
                RosterUnitStatActuel a = actuelParUnite.get(playerId + "|" + idUnit);
                RosterUnitStatObjectif o = objectifParUnite.get(playerId + "|" + idUnit);
                if (a != null) statsActuel.put(idUnit, a);
                if (o != null) statsObjectif.put(idUnit, o);
            }

            StatqResultat resultat = statqCalculationService.calculerPourJoueur(cibles, idUnitParBaseId, statsActuel, statsObjectif);

            resultat.details().forEach(d -> tousLesDetails.add(PlayerStatqDetailActuel.builder()
                    .playerId(playerId).baseId(d.baseId()).team(d.team()).statId(d.statId())
                    .valeurActuelle(d.valeurActuelle()).valeurObjectif(d.valeurObjectif()).variation(d.variation()).note(d.note())
                    .idSync(idSync)
                    .build()));

            PlayerStatqActuel existant = playerStatqActuelRepository.findByPlayerId(playerId).orElse(null);
            if (existant != null) {
                playerStatqHistoriqueRepository.save(PlayerStatqHistorique.builder()
                        .playerId(existant.getPlayerId())
                        .statq(existant.getStatq())
                        .nbStats(existant.getNbStats())
                        .idSync(existant.getIdSync())
                        .build());
            } else {
                existant = new PlayerStatqActuel();
                existant.setPlayerId(playerId);
            }

            existant.setStatq(resultat.statq());
            existant.setNbStats(resultat.nbStats());
            existant.setIdSync(idSync);
            playerStatqActuelRepository.save(existant);

            joueursCalcules++;
        }

        playerStatqDetailActuelRepository.saveAll(tousLesDetails);

        String resultat = String.format("StatQ calculé pour %d joueur(s), %d cible(s) de stat évaluées par joueur",
                joueursCalcules, cibles.size());
        log.info(resultat);
        return resultat;
    }
}