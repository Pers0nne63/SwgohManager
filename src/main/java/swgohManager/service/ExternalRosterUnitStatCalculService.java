package swgohManager.service;

import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.ExternalRosterUnitActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.model.ExternalRosterUnitStatActuel;
import swgohManager.repository.ExternalRosterUnitStatActuelRepository;
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
public class ExternalRosterUnitStatCalculService {

    private final UnitStatReferentialService unitStatReferentialService;
    private final UnitStatCalculationService unitStatCalculationService;
    private final ExternalRosterUnitStatActuelRepository externalRosterUnitStatActuelRepository;

    @Transactional
    public String calculerEtEnregistrer(String playerId, List<ExternalRosterUnitActuel> unites, List<ExternalRosterUnitModActuel> mods) {
        List<String> definitionIds = unites.stream().map(ExternalRosterUnitActuel::getDefinitionId).distinct().toList();
        UnitStatReferentiel ref = unitStatReferentialService.charger(definitionIds);

        Map<String, List<ExternalRosterUnitModActuel>> modsParUnite = mods.stream()
                .collect(Collectors.groupingBy(ExternalRosterUnitModActuel::getIdUnit));

        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        List<UnitCalculResult> resultats = unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> ModAggregationUtil.agregerMods(modsParUnite.getOrDefault(u.idUnit(), List.of()), ref.isDecimalByStat()));

        externalRosterUnitStatActuelRepository.deleteByPlayerId(playerId);
        externalRosterUnitStatActuelRepository.flush();

        List<ExternalRosterUnitStatActuel> entites = resultats.stream()
                .map(r -> mapVersActuel(playerId, r.idUnit(), r.stats()))
                .toList();
        externalRosterUnitStatActuelRepository.saveAll(entites);

        int ignorees = unites.size() - entites.size();
        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (vaisseaux/données manquantes)", entites.size(), ignorees);
        log.info("Stats externes calculées pour {} : {}", playerId, resultat);
        return resultat;
    }

    private ExternalRosterUnitStatActuel mapVersActuel(String playerId, String idUnit, UnitStatResult r) {
        return ExternalRosterUnitStatActuel.builder()
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