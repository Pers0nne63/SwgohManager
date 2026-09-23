package swgohManager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitProgression;
import swgohManager.repository.RosterUnitProgressionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitProgressionService {

    private final RosterUnitProgressionRepository progressionRepository;

    @Transactional
    public void detecterEtEnregistrer(String playerId, List<RosterUnitActuel> anciennesUnites,
            List<RosterUnitActuel> nouvellesUnites, Long idSync) {

        // Premier sync du joueur : pas de baseline, on ne logue rien pour éviter
        // de faire passer tout son roster existant pour des "déblocages" du jour
        if (anciennesUnites.isEmpty()) {
            return;
        }

        Map<String, RosterUnitActuel> anciennesParIdUnit = anciennesUnites.stream()
                .collect(Collectors.toMap(RosterUnitActuel::getIdUnit, u -> u));

        Instant maintenant = Instant.now();
        List<RosterUnitProgression> progressions = new ArrayList<>();

        for (RosterUnitActuel nouvelle : nouvellesUnites) {
            RosterUnitActuel ancienne = anciennesParIdUnit.get(nouvelle.getIdUnit());

            if (ancienne == null) {
                // Vraie unité nouvellement débloquée (baseline existante par ailleurs)
                progressions.add(RosterUnitProgression.builder()
                        .playerId(playerId).idUnit(nouvelle.getIdUnit()).definitionId(nouvelle.getDefinitionId())
                        .dateConstat(maintenant).idSync(idSync)
                        .nouvelleUnite(true)
                        .etoilesApres(nouvelle.getEtoiles())
                        .gearApres(nouvelle.getGear())
                        .relicApres(nouvelle.getRelic())
                        .build());
                continue;
            }

            boolean change = !Objects.equals(ancienne.getEtoiles(), nouvelle.getEtoiles())
                    || !Objects.equals(ancienne.getGear(), nouvelle.getGear())
                    || !Objects.equals(ancienne.getRelic(), nouvelle.getRelic());

            if (change) {
                progressions.add(RosterUnitProgression.builder()
                        .playerId(playerId).idUnit(nouvelle.getIdUnit()).definitionId(nouvelle.getDefinitionId())
                        .dateConstat(maintenant).idSync(idSync)
                        .nouvelleUnite(false)
                        .etoilesAvant(ancienne.getEtoiles()).etoilesApres(nouvelle.getEtoiles())
                        .gearAvant(ancienne.getGear()).gearApres(nouvelle.getGear())
                        .relicAvant(ancienne.getRelic()).relicApres(nouvelle.getRelic())
                        .build());
            }
        }

        if (!progressions.isEmpty()) {
            progressionRepository.saveAll(progressions);
            log.info("{} évolution(s) détectée(s) pour {}", progressions.size(), playerId);
        }
    }
}