package swgohManager.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.dto.pivot.UniteStatInput;
import swgohManager.model.ExternalRosterUnitActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.model.ExternalRosterUnitStatActuel;
import swgohManager.model.ModLigne;
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.model.RosterUnitStatActuel;
import swgohManager.repository.ExternalRosterUnitStatActuelRepository;
import swgohManager.repository.RosterUnitStatActuelRepository;
import swgohManager.service.UnitStatCalculationService.UnitCalculResult;
import swgohManager.service.UnitStatFormulaService.UnitStatResult;
import swgohManager.service.UnitStatReferentialService.UnitStatReferentiel;
import swgohManager.util.ModAggregationUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitStatCalculService {

    private final UnitStatReferentialService unitStatReferentialService;
    private final UnitStatCalculationService unitStatCalculationService;
    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;
    private final ExternalRosterUnitStatActuelRepository externalRosterUnitStatActuelRepository;

    /**
     * Surcharge à 3 arguments pour conserver la compatibilité (ex: appels depuis d'autres services).
     */
    @Transactional
    public String calculerEtEnregistrer(String playerId, List<RosterUnitActuel> unites, List<RosterUnitModActuel> mods) {
        return calculerEtEnregistrer(playerId, unites, mods, null);
    }

    /**
     * Méthode principale à 4 arguments exploitant le cache s'il est fourni (venant de RosterUnitService).
     */
    @Transactional
    public String calculerEtEnregistrer(String playerId, List<RosterUnitActuel> unites, List<RosterUnitModActuel> mods, UnitStatReferentiel cacheRef) {
        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        Map<String, List<ModLigne>> modsParUnite = mods.stream()
                .collect(Collectors.groupingBy(RosterUnitModActuel::getIdUnit,
                        Collectors.mapping(m -> (ModLigne) m, Collectors.toList())));

        // Utilisation du cache partagé s'il est fourni, sinon chargement ciblé en base
        UnitStatReferentiel ref = (cacheRef != null) ? cacheRef : unitStatReferentialService.charger(
                input.stream().map(UniteStatInput::definitionId).distinct().toList());

        List<UnitCalculResult> resultats = unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> ModAggregationUtil.agregerMods(modsParUnite.getOrDefault(u.idUnit(), List.of()), ref.isDecimalByStat()));

        rosterUnitStatActuelRepository.deleteByPlayerId(playerId);
        rosterUnitStatActuelRepository.flush();

        List<RosterUnitStatActuel> entites = resultats.stream()
                .map(r -> mapVersActuel(playerId, r.idUnit(), r.stats()))
                .toList();
        rosterUnitStatActuelRepository.saveAll(entites);

        return logEtRetour(playerId, unites.size(), entites.size(), "");
    }

    @Transactional
    public String calculerEtEnregistrerExterne(String playerId, List<ExternalRosterUnitActuel> unites, List<ExternalRosterUnitModActuel> mods) {
        List<UniteStatInput> input = unites.stream()
                .map(u -> new UniteStatInput(u.getIdUnit(), u.getDefinitionId(), u.getNiveau(), u.getGear(), u.getRelic()))
                .toList();

        Map<String, List<ModLigne>> modsParUnite = mods.stream()
                .collect(Collectors.groupingBy(ExternalRosterUnitModActuel::getIdUnit,
                        Collectors.mapping(m -> (ModLigne) m, Collectors.toList())));

        UnitStatReferentiel ref = unitStatReferentialService.charger(
                input.stream().map(UniteStatInput::definitionId).distinct().toList());

        List<UnitCalculResult> resultats = unitStatCalculationService.calculerPourUnites(input, ref,
                (u, def) -> ModAggregationUtil.agregerMods(modsParUnite.getOrDefault(u.idUnit(), List.of()), ref.isDecimalByStat()));

        externalRosterUnitStatActuelRepository.deleteByPlayerId(playerId);
        externalRosterUnitStatActuelRepository.flush();

        List<ExternalRosterUnitStatActuel> entites = resultats.stream()
                .map(r -> mapVersActuelExterne(playerId, r.idUnit(), r.stats()))
                .toList();
        externalRosterUnitStatActuelRepository.saveAll(entites);

        return logEtRetour(playerId, unites.size(), entites.size(), " externes");
    }

    private String logEtRetour(String playerId, int total, int calculees, String suffixe) {
        int ignorees = total - calculees;
        String resultat = String.format("%d unité(s) calculée(s), %d ignorée(s) (vaisseaux/données manquantes)", calculees, ignorees);
        log.debug("Stats{} calculées pour {} : {}", suffixe, playerId, resultat);
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

    private ExternalRosterUnitStatActuel mapVersActuelExterne(String playerId, String idUnit, UnitStatResult r) {
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

    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            rosterUnitStatActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            rosterUnitStatActuelRepository.flush();
        }
    }
}