package swgohManager.service;

import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.controller.dto.RosterIdUnitProjection;
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

    private final UnitStatPriorityRepository unitStatPriorityRepository;
    private final JoueurRepository joueurRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;
    private final RosterUnitStatObjectifRepository rosterUnitStatObjectifRepository;
    private final PlayerStatqActuelRepository playerStatqActuelRepository;
    private final PlayerStatqHistoriqueRepository playerStatqHistoriqueRepository;
    private final PlayerStatqDetailActuelRepository playerStatqDetailActuelRepository;
    private final SyncExecutionRepository syncExecutionRepository;

    private record StatCible(String baseId, String team, int statId) {}

    @Transactional
    public String calculerPourTousLesJoueurs() {
        List<UnitStatPriority> priorites = unitStatPriorityRepository.findAll();
        List<StatCible> cibles = aplatirCibles(priorites);

        if (cibles.isEmpty()) {
            log.warn("Aucune configuration dans unit_stat_priority — StatQ non calculé");
            return "Aucune configuration StatQ (unit_stat_priority vide)";
        }

        Long idSync = syncExecutionRepository.save(new SyncExecution()).getIdSync();

        // playerId|baseId -> idUnit (résolution une seule fois pour toute la guilde)
        Map<String, String> idUnitParBaseId = rosterUnitActuelRepository.findTousLesIdUnitParBaseId().stream()
                .collect(Collectors.toMap(
                        p -> p.getPlayerId() + "|" + p.getBaseId(), RosterIdUnitProjection::getIdUnit,
                        (a, b) -> a));

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
            List<Integer> notes = new ArrayList<>();

            for (StatCible cible : cibles) {
                String idUnit = idUnitParBaseId.get(playerId + "|" + cible.baseId());

                RosterUnitStatActuel statsActuel = idUnit != null ? actuelParUnite.get(playerId + "|" + idUnit) : null;
                RosterUnitStatObjectif statsObjectif = idUnit != null ? objectifParUnite.get(playerId + "|" + idUnit) : null;

                Double valActuelle = extraireStat(statsActuel, cible.statId());
                Double valObjectif = extraireStat(statsObjectif, cible.statId());

                Double variation = null;
                int note;

                if (valActuelle == null || valObjectif == null || valObjectif == 0) {
                    note = 0;
                } else {
                    variation = (valActuelle - valObjectif) / valObjectif;
                    note = attribuerNote(variation);
                }

                notes.add(note);

                tousLesDetails.add(PlayerStatqDetailActuel.builder()
                        .playerId(playerId).baseId(cible.baseId()).team(cible.team()).statId(cible.statId())
                        .valeurActuelle(valActuelle).valeurObjectif(valObjectif).variation(variation).note(note)
                        .idSync(idSync)
                        .build());
            }

            double statq = notes.stream().mapToInt(Integer::intValue).sum();

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

            existant.setStatq(statq);
            existant.setNbStats(notes.size());
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

    private List<StatCible> aplatirCibles(List<UnitStatPriority> priorites) {
        List<StatCible> cibles = new ArrayList<>();
        for (UnitStatPriority p : priorites) {
            ajouterSiPresent(cibles, p, p.getStatId1());
            ajouterSiPresent(cibles, p, p.getStatId2());
            ajouterSiPresent(cibles, p, p.getStatId3());
            ajouterSiPresent(cibles, p, p.getStatId4());
        }
        return cibles;
    }

    private void ajouterSiPresent(List<StatCible> cibles, UnitStatPriority p, Integer statId) {
        if (statId != null) {
            cibles.add(new StatCible(p.getBaseId(), p.getTeam(), statId));
        }
    }

    private Double extraireStat(UnitStatValues s, int statId) {
        if (s == null) return null;
        return switch (statId) {
            case 1 -> s.getSante();
            case 5 -> s.getVitesse();
            case 6 -> s.getAttaquePhysique();
            case 7 -> s.getAttaqueSpeciale();
            case 8 -> s.getArmure();
            case 9 -> s.getResistance();
            case 14 -> s.getCcPhysique();
            case 15 -> s.getCcSpeciaux();
            case 17 -> s.getPouvoir();
            case 18 -> s.getTenacite();
            case 28 -> s.getProtection();
            default -> null;
        };
    }

    private int attribuerNote(double variation) {
        if (variation > 0) return 4;
        if (variation > -0.025) return 3;
        if (variation > -0.05) return 2;
        if (variation > -0.075) return 1;
        return 0;
    }
}