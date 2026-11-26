package swgohManager.controller.web;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TbEventRepository;
import swgohManager.repository.TerritoryBattleRepository;

/** Page d'analyse des événements de zone (tb-events) d'une Territory Battle. */
@Controller
@RequiredArgsConstructor
public class TbEventAnalyseWebController {

    private static final int NB_PHASES = 6;
    private static final int SLOTS_PAR_PHASE = 48;                  // 24 h / 30 min
    private static final int NB_SLOTS = NB_PHASES * SLOTS_PAR_PHASE;
    private static final long SLOT_MS = 30L * 60 * 1000;

    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbEventRepository tbEventRepository;

    /** Nombre d'événements d'un type dans une tranche de 30 min. */
    public record Cellule(int slot, String type, long nb) {}

    /** Données JSON passées au JavaScript de la page. */
    public record Analyse(long debutMs, long slotMs, int nbSlots, int slotsParPhase, List<Cellule> cellules) {}

    @GetMapping("/tb-events")
    public String analyse(@RequestParam(required = false) Long tbId, Model model) {
        List<TerritoryBattle> tbs = territoryBattleRepository.findAllByOrderByStartTimeDesc();

        // Sans paramètre : la TB la plus récente qui a une date de début
        TerritoryBattle tb = tbs.stream()
                .filter(t -> tbId != null ? Objects.equals(t.getId(), tbId) : t.getStartTime() != null)
                .findFirst()
                .orElse(null);

        model.addAttribute("territoryBattles", tbs);
        model.addAttribute("tbIdSelectionnee", tb != null ? tb.getId() : null);

        if (tb != null && tb.getStartTime() != null) {
            Instant debut = tb.getStartTime();
            Instant fin = debut.plus(Duration.ofHours(NB_PHASES * 24L));

            List<Cellule> cellules = tbEventRepository
                    .repartitionParTrancheEtType(tb.getInstanceId(), debut, fin).stream()
                    .map(r -> new Cellule(r.getSlot(), r.getType(), r.getNb()))
                    .toList();

            model.addAttribute("analyse", new Analyse(debut.toEpochMilli(), SLOT_MS, NB_SLOTS, SLOTS_PAR_PHASE, cellules));
            model.addAttribute("nbEvenements", cellules.stream().mapToLong(Cellule::nb).sum());
        }
        return "tb-events";
    }
}