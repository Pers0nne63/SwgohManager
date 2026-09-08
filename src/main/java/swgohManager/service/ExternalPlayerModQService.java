package swgohManager.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.ExternalPlayerModQActuel;
import swgohManager.model.ExternalRosterUnitModActuel;
import swgohManager.repository.ExternalPlayerModQActuelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPlayerModQService {

    private final ExternalPlayerModQActuelRepository externalPlayerModQActuelRepository;
    private final ModQCalculationService modQCalculationService; // NOUVELLE dépendance

    @Transactional
    public void calculerEtEnregistrer(String playerId, List<ExternalRosterUnitModActuel> modsActuels, Long gpChar) {

        List<ModQCalculationService.ModSecondaryValue> valeurs = modsActuels.stream()
                .map(m -> new ModQCalculationService.ModSecondaryValue(m.getIdSecondaire(), m.getValeurSecondaire()))
                .collect(Collectors.toList());

        ModQCalculationService.ModSpeedCounts counts = modQCalculationService.calculerRepartitionVitesse(valeurs);
        Double modQ = modQCalculationService.calculerModQ(counts, gpChar);

        externalPlayerModQActuelRepository.deleteByPlayerId(playerId);
        externalPlayerModQActuelRepository.flush();

        ExternalPlayerModQActuel entite = ExternalPlayerModQActuel.builder()
                .playerId(playerId)
                .mod25Plus(counts.mod25Plus())
                .mod20_24(counts.mod20_24())
                .mod15_19(counts.mod15_19())
                .mod10_14(counts.mod10_14())
                .modQ(modQ)
                .build();

        externalPlayerModQActuelRepository.save(entite);
        log.info("ModQ externe calculé pour {} : {}", playerId, modQ);
    }
}