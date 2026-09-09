package swgohManager.service;

import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.service.UnitStatCalculationService.UnitCalculResult;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
import swgohManager.service.UnitStatReferentialService.UnitStatReferentiel;
import swgohManager.util.ModAggregationUtil.ModAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitStatObjectifService {

    private final JoueurRepository joueurRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final LeaderboardModMoyRepository leaderboardModMoyRepository;
    private final RosterUnitStatObjectifRepository rosterUnitStatObjectifRepository;
    private final ExternalRosterUnitStatObjectifRepository externalRosterUnitStatObjectifRepository;
    private final UnitStatReferentialService unitStatReferentialService;
    private final UnitStatCalculationService unitStatCalculationService;

    /** Portée guilde : calcul batch pour tous les joueurs présents (déclenché hors du flux de sync individuel). */
    @Transactional
    public String calculerPourTousLesJoueurs() {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();
        UnitStatReferentiel ref = unitStatReferentialService.chargerComplet();
        Map<String, LeaderboardModMoy> modMoyByBaseId = chargerModMoy();

        int totalCalculees = 0, totalIgnorees = 0;

        for (Joueur joueur : joueurs) {
            List<RosterUnitActuel> unites = rosterUnitActuelRepository.findByPlayerId(joueur.getPlayerId());

            List<UniteStatInput> input = unites.stream()
                    .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                    .toList();

            List<UnitCalculResult> resultats = calculerAvecModMoy(input, ref, modMoyByBaseId);

            rosterUnitStatObjectifRepository.deleteByPlayerId(joueur.getPlayerId());
            rosterUnitStatObjectifRepository.flush();

            List<RosterUnitStatObjectif> entites = resultats.stream()
                    .map(r -> mapVersObjectif(joueur.getPlayerId(), r.idUnit(), r.stats()))
                    .toList();
            rosterUnitStatObjectifRepository.saveAll(entites);

            totalCalculees += entites.size();
            totalIgnorees += (unites.size() - entites.size());
        }

        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (données manquantes ou sans référence leaderboard)",
                totalCalculees, totalIgnorees);
        log.info(resultat);
        return resultat;
    }

    /** Portée externe : calcul pour un seul joueur, déclenché au moment du scan. */
    @Transactional
    public String calculerEtEnregistrerExterne(String playerId, List<ExternalRosterUnitActuel> unites) {
        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        UnitStatReferentiel ref = unitStatReferentialService.charger(
                input.stream().map(UniteStatInput::definitionId).distinct().toList());
        Map<String, LeaderboardModMoy> modMoyByBaseId = chargerModMoy();

        List<UnitCalculResult> resultats = calculerAvecModMoy(input, ref, modMoyByBaseId);

        externalRosterUnitStatObjectifRepository.deleteByPlayerId(playerId);
        externalRosterUnitStatObjectifRepository.flush();

        List<ExternalRosterUnitStatObjectif> entites = resultats.stream()
                .map(r -> mapVersObjectifExterne(playerId, r.idUnit(), r.stats()))
                .toList();
        externalRosterUnitStatObjectifRepository.saveAll(entites);

        int ignorees = unites.size() - entites.size();
        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (données manquantes ou sans référence leaderboard)",
                entites.size(), ignorees);
        log.info("Objectifs externes calculés pour {} : {}", playerId, resultat);
        return resultat;
    }

    private Map<String, LeaderboardModMoy> chargerModMoy() {
        return leaderboardModMoyRepository.findAll().stream()
                .collect(Collectors.toMap(LeaderboardModMoy::getBaseId, m -> m));
    }

    private List<UnitCalculResult> calculerAvecModMoy(List<UniteStatInput> input, UnitStatReferentiel ref,
                                                        Map<String, LeaderboardModMoy> modMoyByBaseId) {
        return unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> {
                    LeaderboardModMoy modMoy = modMoyByBaseId.get(def.getBaseId());
                    return modMoy != null ? mapVersAccumulator(modMoy) : null;
                });
    }

    private ModAccumulator mapVersAccumulator(LeaderboardModMoy m) {
        ModAccumulator acc = new ModAccumulator();
        acc.speed = m.getSpeed(); acc.pSpeed = m.getPSpeed();
        acc.pOff = m.getPOff(); acc.fOff = m.getFOff();
        acc.pSante = m.getPSante(); acc.fSante = m.getFSante();
        acc.pProt = m.getPProt(); acc.fProt = m.getFProt();
        acc.pDef = m.getPDef(); acc.fDef = m.getFDef();
        acc.pot = m.getPot(); acc.ten = m.getTen();
        acc.cc = m.getCc(); acc.dc = m.getDc();
        acc.critAvoid = m.getCritAvoid(); acc.acc = m.getAcc();
        return acc;
    }

    private RosterUnitStatObjectif mapVersObjectif(String playerId, String idUnit, UnitStatResult r) {
        return RosterUnitStatObjectif.builder()
                .playerId(playerId).idUnit(idUnit)
                .sante(r.sante()).protection(r.protection()).vitesse(r.vitesse())
                .attaquePhysique(r.attaquePhysique()).attaqueSpeciale(r.attaqueSpeciale())
                .armure(r.armure()).resistance(r.resistance())
                .penetrationArmure(r.penetrationArmure()).penetrationResistance(r.penetrationResistance())
                .esquive(r.esquive()).deviation(r.deviation())
                .ccPhysique(r.ccPhysique()).ccSpeciaux(r.ccSpeciaux())
                .degatsCritiques(r.degatsCritiques()).pouvoir(r.pouvoir()).tenacite(r.tenacite())
                .volDeSante(r.volDeSante())
                .precisionPhysique(r.precisionPhysique()).precisionSpeciale(r.precisionSpeciale())
                .esquiveCritiquePhysique(r.esquiveCritiquePhysique()).esquiveCritiqueSpeciale(r.esquiveCritiqueSpeciale())
                .defense(r.defense())
                .build();
    }

    private ExternalRosterUnitStatObjectif mapVersObjectifExterne(String playerId, String idUnit, UnitStatResult r) {
        return ExternalRosterUnitStatObjectif.builder()
                .playerId(playerId).idUnit(idUnit)
                .sante(r.sante()).protection(r.protection()).vitesse(r.vitesse())
                .attaquePhysique(r.attaquePhysique()).attaqueSpeciale(r.attaqueSpeciale())
                .armure(r.armure()).resistance(r.resistance())
                .penetrationArmure(r.penetrationArmure()).penetrationResistance(r.penetrationResistance())
                .esquive(r.esquive()).deviation(r.deviation())
                .ccPhysique(r.ccPhysique()).ccSpeciaux(r.ccSpeciaux())
                .degatsCritiques(r.degatsCritiques()).pouvoir(r.pouvoir()).tenacite(r.tenacite())
                .volDeSante(r.volDeSante())
                .precisionPhysique(r.precisionPhysique()).precisionSpeciale(r.precisionSpeciale())
                .esquiveCritiquePhysique(r.esquiveCritiquePhysique()).esquiveCritiqueSpeciale(r.esquiveCritiqueSpeciale())
                .defense(r.defense())
                .build();
    }

    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            rosterUnitStatObjectifRepository.deleteByPlayerIdNotIn(joueursActifs);
            rosterUnitStatObjectifRepository.flush();
        }
    }
}