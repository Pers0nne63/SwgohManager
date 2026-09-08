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
public class ExternalStatqCalculService {

    private final ExternalRosterUnitActuelRepository externalRosterUnitActuelRepository;
    private final ExternalRosterUnitStatActuelRepository externalRosterUnitStatActuelRepository;
    private final ExternalRosterUnitStatObjectifRepository externalRosterUnitStatObjectifRepository;
    private final ExternalPlayerStatqActuelRepository externalPlayerStatqActuelRepository;
    private final ExternalPlayerStatqDetailActuelRepository externalPlayerStatqDetailActuelRepository;
    private final StatqCalculationService statqCalculationService;

    @Transactional
    public String calculerEtEnregistrer(String playerId) {
        List<StatCible> cibles = statqCalculationService.chargerCibles();
        if (cibles.isEmpty()) {
            log.warn("Aucune configuration dans unit_stat_priority — StatQ externe non calculé pour {}", playerId);
            return "Aucune configuration StatQ (unit_stat_priority vide)";
        }

        Map<String, String> idUnitParBaseId = externalRosterUnitActuelRepository.findIdUnitParBaseId(playerId).stream()
                .collect(Collectors.toMap(RosterIdUnitProjection::getBaseId, RosterIdUnitProjection::getIdUnit, (a, b) -> a));

        Map<String, UnitStatValues> statsActuel = externalRosterUnitStatActuelRepository.findByPlayerId(playerId).stream()
                .collect(Collectors.toMap(ExternalRosterUnitStatActuel::getIdUnit, s -> (UnitStatValues) s, (a, b) -> a));

        Map<String, UnitStatValues> statsObjectif = externalRosterUnitStatObjectifRepository.findByPlayerId(playerId).stream()
                .collect(Collectors.toMap(ExternalRosterUnitStatObjectif::getIdUnit, s -> (UnitStatValues) s, (a, b) -> a));

        StatqResultat resultat = statqCalculationService.calculerPourJoueur(cibles, idUnitParBaseId, statsActuel, statsObjectif);

        externalPlayerStatqDetailActuelRepository.deleteByPlayerId(playerId);
        externalPlayerStatqDetailActuelRepository.flush();

        List<ExternalPlayerStatqDetailActuel> details = resultat.details().stream()
                .map(d -> ExternalPlayerStatqDetailActuel.builder()
                        .playerId(playerId).baseId(d.baseId()).team(d.team()).statId(d.statId())
                        .valeurActuelle(d.valeurActuelle()).valeurObjectif(d.valeurObjectif()).variation(d.variation()).note(d.note())
                        .build())
                .toList();
        externalPlayerStatqDetailActuelRepository.saveAll(details);

        ExternalPlayerStatqActuel entite = externalPlayerStatqActuelRepository.findByPlayerId(playerId).orElse(new ExternalPlayerStatqActuel());
        entite.setPlayerId(playerId);
        entite.setStatq(resultat.statq());
        entite.setNbStats(resultat.nbStats());
        externalPlayerStatqActuelRepository.save(entite);

        String message = String.format("StatQ externe calculé pour %s : %.1f (%d stat(s))", playerId, resultat.statq(), resultat.nbStats());
        log.info(message);
        return message;
    }
}