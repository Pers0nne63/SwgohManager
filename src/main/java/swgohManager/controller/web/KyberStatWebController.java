package swgohManager.controller.web;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.model.KyberStatLigne;
import swgohManager.model.KyberStatSnapshot;
import swgohManager.repository.KyberStatLigneRepository;
import swgohManager.repository.KyberStatSnapshotRepository;

@Controller
@RequiredArgsConstructor
public class KyberStatWebController {

    private final KyberStatSnapshotRepository snapshotRepository;
    private final KyberStatLigneRepository ligneRepository;

    public record DivisionRow(int division, int nbJoueurs, double pct, int barre, long pgMoyenne) {}

    @GetMapping("/stat-gac")
    public String afficher(@RequestParam(required = false) Long snapshotId, Model model) {
        List<KyberStatSnapshot> recents = snapshotRepository.findTop26ByOrderByDateSnapshotDesc();
        model.addAttribute("snapshots", recents);
        if (recents.isEmpty()) {
            return "stat-gac";
        }

        KyberStatSnapshot courant = recents.stream()
                .filter(s -> s.getId().equals(snapshotId))
                .findFirst()
                .orElse(recents.get(0));
        model.addAttribute("snapshot", courant);

        List<KyberStatLigne> lignes = ligneRepository.findBySnapshotIdOrderByTypeAscCleAsc(courant.getId());
        List<KyberStatLigne> lignesDiv = lignes.stream().filter(l -> "DIVISION".equals(l.getType())).toList();
        List<KyberStatLigne> tranches = lignes.stream().filter(l -> "TRANCHE_SR".equals(l.getType())).toList();

        // --- Divisions ---
        int totalDiv = lignesDiv.stream().mapToInt(KyberStatLigne::getNbJoueurs).sum();
        int maxDiv = lignesDiv.stream().mapToInt(KyberStatLigne::getNbJoueurs).max().orElse(1);
        List<DivisionRow> divisions = lignesDiv.stream()
                .map(l -> new DivisionRow(
                        l.getCle(),
                        l.getNbJoueurs(),
                        totalDiv > 0 ? l.getNbJoueurs() * 100.0 / totalDiv : 0,
                        l.getNbJoueurs() * 100 / Math.max(maxDiv, 1),
                        l.getPgMoyenne()))
                .toList();
        model.addAttribute("divisions", divisions);

        long pgGlobale = totalDiv > 0
                ? Math.round(lignesDiv.stream().mapToDouble(l -> (double) l.getNbJoueurs() * l.getPgMoyenne()).sum() / totalDiv)
                : 0;
        model.addAttribute("pgMoyenneGlobale", pgGlobale);

        // --- Tranches de skill rating ---
        model.addAttribute("tranches", tranches);
        model.addAttribute("maxTranche", tranches.stream().mapToInt(KyberStatLigne::getNbJoueurs).max().orElse(1));
        model.addAttribute("trancheLabels", tranches.stream().map(t -> t.getCle() + "-" + (t.getCle() + 99)).toList());
        model.addAttribute("trancheNb", tranches.stream().map(KyberStatLigne::getNbJoueurs).toList());
        model.addAttribute("tranchePg", tranches.stream().map(KyberStatLigne::getPgMoyenne).toList());

        // --- Évolution hebdo (nb de joueurs par division) ---
        List<KyberStatSnapshot> chrono = new ArrayList<>(recents);
        Collections.reverse(chrono);
        List<Long> ids = chrono.stream().map(KyberStatSnapshot::getId).toList();

        Map<Long, Map<Integer, Integer>> parSnapshot = ligneRepository.findBySnapshotIdInAndType(ids, "DIVISION").stream()
                .collect(Collectors.groupingBy(KyberStatLigne::getSnapshotId,
                        Collectors.toMap(KyberStatLigne::getCle, KyberStatLigne::getNbJoueurs)));

        Map<Integer, List<Integer>> evoSeries = new LinkedHashMap<>();
        for (int d = 1; d <= 5; d++) {
            final int division = d;
            evoSeries.put(d, chrono.stream()
                    .map(s -> parSnapshot.getOrDefault(s.getId(), Map.of()).getOrDefault(division, 0))
                    .toList());
        }
        model.addAttribute("evoLabels", chrono.stream()
                .map(s -> s.getDateSnapshot().format(DateTimeFormatter.ofPattern("dd/MM")))
                .toList());
        model.addAttribute("evoSeries", evoSeries);

        return "stat-gac";
    }
}