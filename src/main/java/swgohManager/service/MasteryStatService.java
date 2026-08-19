package swgohManager.service;

import swgohManager.model.MasteryStat;
import swgohManager.repository.MasteryStatRepository;
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
public class MasteryStatService {

    private final MasteryStatRepository masteryStatRepository;

    private record Entree(String classe, int unitStatId, double value) {}

    // Valeurs fixes issues du référentiel communautaire — ne devraient jamais évoluer.
    private static final List<Entree> DONNEES = List.of(
            new Entree("AGI_ATK", 14, 0.3), new Entree("AGI_ATK", 15, 0.3), new Entree("AGI_ATK", 16, 0.3),
            new Entree("AGI_ATK", 6, 26), new Entree("AGI_ATK", 7, 26),

            new Entree("AGI_HLR", 14, 0.45), new Entree("AGI_HLR", 15, 0.45),
            new Entree("AGI_HLR", 12, 0.35), new Entree("AGI_HLR", 13, 0.35), new Entree("AGI_HLR", 1, 225),

            new Entree("AGI_TNK", 12, 0.35), new Entree("AGI_TNK", 13, 0.35),
            new Entree("AGI_TNK", 1, 120), new Entree("AGI_TNK", 28, 200),

            new Entree("AGI_SUP", 14, 0.3), new Entree("AGI_SUP", 15, 0.3), new Entree("AGI_SUP", 16, 0.3),
            new Entree("AGI_SUP", 12, 0.35), new Entree("AGI_SUP", 13, 0.35),

            new Entree("STR_ATK", 37, 0.3), new Entree("STR_ATK", 38, 0.3),
            new Entree("STR_ATK", 6, 26), new Entree("STR_ATK", 7, 26),
            new Entree("STR_ATK", 10, 2), new Entree("STR_ATK", 11, 2),

            new Entree("STR_HLR", 8, 0.06), new Entree("STR_HLR", 27, 0.0), new Entree("STR_HLR", 1, 420),

            new Entree("STR_TNK", 8, 0.1), new Entree("STR_TNK", 27, 0.15), new Entree("STR_TNK", 28, 220),

            new Entree("STR_SUP", 37, 0.3), new Entree("STR_SUP", 38, 0.3),
            new Entree("STR_SUP", 10, 2), new Entree("STR_SUP", 11, 2), new Entree("STR_SUP", 27, 0.15),

            new Entree("TAC_ATK", 37, 0.3), new Entree("TAC_ATK", 38, 0.3),
            new Entree("TAC_ATK", 14, 0.3), new Entree("TAC_ATK", 15, 0.3),
            new Entree("TAC_ATK", 6, 27), new Entree("TAC_ATK", 7, 27),

            new Entree("TAC_HLR", 39, 0.3), new Entree("TAC_HLR", 40, 0.3),
            new Entree("TAC_HLR", 9, 0.1), new Entree("TAC_HLR", 1, 300),

            new Entree("TAC_TNK", 39, 0.45), new Entree("TAC_TNK", 40, 0.45),
            new Entree("TAC_TNK", 28, 230), new Entree("TAC_TNK", 9, 0.1),

            new Entree("TAC_SUP", 37, 0.3), new Entree("TAC_SUP", 38, 0.3),
            new Entree("TAC_SUP", 39, 0.3), new Entree("TAC_SUP", 40, 0.3),
            new Entree("TAC_SUP", 6, 12), new Entree("TAC_SUP", 7, 12)
    );

    @Transactional
    public String seedDonnees() {
        Map<String, MasteryStat> existantes = masteryStatRepository.findAll().stream()
                .collect(Collectors.toMap(m -> m.getMasteryClass() + "|" + m.getUnitStatId(), m -> m));

        List<MasteryStat> aSauvegarder = new ArrayList<>();

        for (Entree e : DONNEES) {
            String cle = e.classe() + "|" + e.unitStatId();
            MasteryStat m = existantes.get(cle);
            if (m == null) {
                m = new MasteryStat();
                m.setMasteryClass(e.classe());
                m.setUnitStatId(e.unitStatId());
                existantes.put(cle, m);
            }
            m.setValue(e.value());
            aSauvegarder.add(m);
        }

        masteryStatRepository.saveAll(aSauvegarder);

        String resultat = String.format("%d ligne(s) de mastery_stat chargée(s)", aSauvegarder.size());
        log.info(resultat);
        return resultat;
    }
}