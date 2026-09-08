package swgohManager.service;

import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.*;
import swgohManager.repository.*;
import swgohManager.service.UnitStatCalculationService.UnitCalculResult;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
import swgohManager.service.UnitStatReferentialService.UnitStatReferentiel;
import swgohManager.util.ModAggregationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitStatCalculService {

    private final UnitStatReferentialService unitStatReferentialService;
    private final UnitStatCalculationService unitStatCalculationService;
    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;

    @Transactional
    public String calculerEtEnregistrer(String playerId, List<RosterUnitActuel> unites, List<RosterUnitModActuel> mods) {
        List<String> definitionIds = unites.stream().map(RosterUnitActuel::getDefinitionId).distinct().toList();
        UnitStatReferentiel ref = unitStatReferentialService.charger(definitionIds);

        Map<String, List<RosterUnitModActuel>> modsParUnite = mods.stream()
                .collect(Collectors.groupingBy(RosterUnitModActuel::getIdUnit));

        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        List<UnitCalculResult> resultats = unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> ModAggregationUtil.agregerMods(modsParUnite.getOrDefault(u.idUnit(), List.of()), ref.isDecimalByStat()));

        rosterUnitStatActuelRepository.deleteByPlayerId(playerId);
        rosterUnitStatActuelRepository.flush();

        List<RosterUnitStatActuel> entites = resultats.stream()
                .map(r -> mapVersActuel(playerId, r.idUnit(), r.stats()))
                .toList();
        rosterUnitStatActuelRepository.saveAll(entites);

        int ignorees = unites.size() - entites.size();
        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (vaisseaux/données manquantes)", entites.size(), ignorees);
        log.info("Stats calculées pour {} : {}", playerId, resultat);
        return resultat;
    }

    private RosterUnitStatActuel mapVersActuel(String playerId, String idUnit, UnitStatResult r) {
        return RosterUnitStatActuel.builder()
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
            rosterUnitStatActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            rosterUnitStatActuelRepository.flush();
        }
    }
}