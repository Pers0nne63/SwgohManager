package swgohManager.service;

import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.StatProgressionRaw;
import swgohManager.model.StatProgression;
import swgohManager.repository.StatProgressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatProgressionService {

    private final SwgohDataClient swgohDataClient;
    private final StatProgressionRepository statProgressionRepository;

    @Transactional
    public String synchroniserStatProgression() {
        String version = swgohDataClient.getLatestGameVersion();
        List<StatProgressionRaw> lignesBrutes = swgohDataClient.streamStatProgressionSegment(version);

        Map<String, StatProgression> existantes = statProgressionRepository.findAll().stream()
                .collect(Collectors.toMap(sp -> sp.getStatProgressionId() + "|" + sp.getUnitStatId(), sp -> sp));

        List<StatProgression> aSauvegarder = new ArrayList<>();
        int nouveaux = 0, misAJour = 0, doublonsIgnores = 0;

        for (StatProgressionRaw raw : lignesBrutes) {
            if (raw.stat() == null || raw.stat().stat() == null) {
                continue;
            }

            for (StatProgressionRaw.StatEntry entry : raw.stat().stat()) {
                String cle = raw.id() + "|" + entry.unitStatId();
                StatProgression sp = existantes.get(cle);

                if (sp == null) {
                    sp = new StatProgression();
                    sp.setStatProgressionId(raw.id());
                    sp.setUnitStatId(entry.unitStatId());
                    existantes.put(cle, sp); // évite un doublon si la même clé réapparaît plus loin dans le JSON
                    nouveaux++;
                    aSauvegarder.add(sp);
                } else if (!aSauvegarder.contains(sp)) {
                    misAJour++;
                    aSauvegarder.add(sp);
                } else {
                    doublonsIgnores++;
                }

                sp.setValeur(parseLong(entry.unscaledDecimalValue()));
                sp.setGameVersion(version);
            }
        }

        statProgressionRepository.saveAll(aSauvegarder);

        String resultat = String.format("Version %s : %d nouvelle(s) ligne(s), %d mise(s) à jour, %d doublon(s) dans le JSON ignoré(s)",
                version, nouveaux, misAJour, doublonsIgnores);
        log.info(resultat);
        return resultat;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique : {}", value);
            return null;
        }
    }
}