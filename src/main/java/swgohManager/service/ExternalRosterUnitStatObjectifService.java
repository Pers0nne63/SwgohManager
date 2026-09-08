package swgohManager.service;

import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.ExternalRosterUnitActuel;
import swgohManager.model.ExternalRosterUnitStatObjectif;
import swgohManager.model.LeaderboardModMoy;
import swgohManager.repository.ExternalRosterUnitStatObjectifRepository;
import swgohManager.repository.LeaderboardModMoyRepository;
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
public class ExternalRosterUnitStatObjectifService {

    private final LeaderboardModMoyRepository leaderboardModMoyRepository;
    private final ExternalRosterUnitStatObjectifRepository externalRosterUnitStatObjectifRepository;
    private final UnitStatReferentialService unitStatReferentialService;
    private final UnitStatCalculationService unitStatCalculationService;

    @Transactional
    public String calculerEtEnregistrer(String playerId, List<ExternalRosterUnitActuel> unites) {
        List<String> definitionIds = unites.stream().map(ExternalRosterUnitActuel::getDefinitionId).distinct().toList();
        UnitStatReferentiel ref = unitStatReferentialService.charger(definitionIds);

        Map<String, LeaderboardModMoy> modMoyByBaseId = leaderboardModMoyRepository.findAll().stream()
                .collect(Collectors.toMap(LeaderboardModMoy::getBaseId, m -> m));

        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        List<UnitCalculResult> resultats = unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> {
                    LeaderboardModMoy modMoy = modMoyByBaseId.get(def.getBaseId());
                    return modMoy != null ? mapVersAccumulator(modMoy) : null;
                });

        externalRosterUnitStatObjectifRepository.deleteByPlayerId(playerId);
        externalRosterUnitStatObjectifRepository.flush();

        List<ExternalRosterUnitStatObjectif> entites = resultats.stream()
                .map(r -> mapVersObjectif(playerId, r.idUnit(), r.stats()))
                .toList();
        externalRosterUnitStatObjectifRepository.saveAll(entites);

        int ignorees = unites.size() - entites.size();
        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (données manquantes ou sans référence leaderboard)",
                entites.size(), ignorees);
        log.info("Objectifs externes calculés pour {} : {}", playerId, resultat);
        return resultat;
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

    private ExternalRosterUnitStatObjectif mapVersObjectif(String playerId, String idUnit, UnitStatResult r) {
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
}