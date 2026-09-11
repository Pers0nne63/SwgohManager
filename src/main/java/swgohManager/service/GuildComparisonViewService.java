package swgohManager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import swgohManager.model.GuildComparisonResult;
import swgohManager.repository.GuildComparisonResultRepository;

@Service
@RequiredArgsConstructor
public class GuildComparisonViewService {

    private final GuildComparisonResultRepository guildComparisonResultRepository;
    private final ObjectMapper objectMapper;

    public record GuildeInfo(String guildId, String nom, Integer nbMembres, String galacticPower) {}
    public record MetricRow(String label, String icon, String valueA, String valueB, String winner) {}
    public record RelicTier(String label, String value, boolean winner) {}
    public record StatqTeamRow(String team, String valueA, String valueB, String winner) {}
    public record GuildComparisonVM(
            GuildeInfo guildeA, GuildeInfo guildeB, Instant dateComparaison,
            List<MetricRow> metrics, List<List<RelicTier>> relicCotes, List<StatqTeamRow> statQParTeam
    ) {}
    
    @Transactional(readOnly = true)
    public GuildComparisonVM construire(String guildIdB) {
        GuildComparisonResult r = guildComparisonResultRepository.findByGuildIdB(guildIdB).orElse(null);
        if (r == null) return null;

        List<MetricRow> metrics = List.of(
                // "Puissance galactique" et "Membres" retirés d'ici, remontés dans le header
                metric("Étoiles dernière BT", "bi-star-fill", r.getEtoilesBtA(), r.getEtoilesBtB()),
                metric("Score dernier raid", "bi-lightning-fill", r.getScoreRaidTotalA(), r.getScoreRaidTotalB()),
                metricDecimal("ModQ moyen", "bi-hexagon-fill", r.getModQMoyenA(), r.getModQMoyenB()),
                metric("Mods 25+", "bi-star", r.getMod25PlusA(), r.getMod25PlusB()),
                metric("Mods 20-24", "bi-star-half", r.getMod20A24A(), r.getMod20A24B()),
                metric("Mods 15-19", "bi-star", r.getMod15A19A(), r.getMod15A19B()),
                metricDecimal("StatQ moyen", "bi-graph-up", r.getStatQMoyenA(), r.getStatQMoyenB()),
                metric("FDTC", "bi-gem", r.getNbFdtcA(), r.getNbFdtcB()),
                metric("DTC niveau 9", "bi-gem", r.getNbDtc9A(), r.getNbDtc9B()),
                metric("Omicrons TB", "bi-cpu-fill", r.getNbOmicronTbA(), r.getNbOmicronTbB()),
                metric("Omicrons TW", "bi-cpu", r.getNbOmicronTwA(), r.getNbOmicronTwB())
        );

        List<List<RelicTier>> relicCotes = construireRelicTiers(r);
        
        List<StatqTeamRow> statQParTeam = fusionnerStatqTeams(r.getStatQParTeamJsonA(), r.getStatQParTeamJsonB());

        return new GuildComparisonVM(
                new GuildeInfo(r.getGuildIdA(), r.getGuildNomA(), r.getNbMembresA(), formatPg(r.getGalacticPowerTotalA())),
                new GuildeInfo(r.getGuildIdB(), r.getGuildNomB(), r.getNbMembresB(), formatPg(r.getGalacticPowerTotalB())),
                r.getDateComparaison(), metrics, relicCotes, statQParTeam
        );
    }
    
    private String formatPg(Long pg) {
        return pg != null ? String.format("%,d", pg).replace(',', ' ') : "-";
    }

    private MetricRow metric(String label, String icon, Number a, Number b) {
        return new MetricRow(label, icon, txt(a), txt(b), comparerNumbers(a, b));
    }

    private MetricRow metricDecimal(String label, String icon, Double a, Double b) {
        return new MetricRow(label, icon,
                a != null ? String.format("%.2f", a) : "-",
                b != null ? String.format("%.2f", b) : "-",
                comparerNumbers(a, b));
    }

    private String comparerNumbers(Number a, Number b) {
        if (a == null || b == null) return null;
        int cmp = Double.compare(a.doubleValue(), b.doubleValue());
        return cmp == 0 ? null : (cmp > 0 ? "A" : "B");
    }

    private String txt(Number n) {
        if (n == null) return "-";
        return String.format("%,d", n.longValue()).replace(',', ' ');
    }

    private List<List<RelicTier>> construireRelicTiers(GuildComparisonResult r) {
        record Paire(String label, Long a, Long b) {}
        List<Paire> paires = List.of(
                new Paire("R10", r.getRelic10A(), r.getRelic10B()),
                new Paire("R9", r.getRelic9A(), r.getRelic9B()),
                new Paire("R8", r.getRelic8A(), r.getRelic8B()),
                new Paire("R6-7", r.getRelic6Et7A(), r.getRelic6Et7B()),
                new Paire("R0-5", r.getRelic0A5A(), r.getRelic0A5B()),
                new Paire("G1-13", r.getSansRelicA(), r.getSansRelicB())
        );

        List<RelicTier> aOut = new ArrayList<>(), bOut = new ArrayList<>();
        for (Paire p : paires) {
            long va = p.a() != null ? p.a() : 0;
            long vb = p.b() != null ? p.b() : 0;
            aOut.add(new RelicTier(p.label(), txt(p.a()), va > vb));
            bOut.add(new RelicTier(p.label(), txt(p.b()), vb > va));
        }
        return List.of(aOut, bOut);
    }

    private List<StatqTeamRow> fusionnerStatqTeams(String jsonA, String jsonB) {
        Map<String, Double> mapA = parserTeams(jsonA), mapB = parserTeams(jsonB);
        Set<String> teams = new TreeSet<>();
        teams.addAll(mapA.keySet());
        teams.addAll(mapB.keySet());
        List<StatqTeamRow> rows = new ArrayList<>();
        for (String team : teams) {
            Double a = mapA.get(team), b = mapB.get(team);
            rows.add(new StatqTeamRow(team,
                    a != null ? String.format("%.2f", a) : "-",
                    b != null ? String.format("%.2f", b) : "-",
                    comparerNumbers(a, b)));
        }
        return rows;
    }

    private Map<String, Double> parserTeams(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            List<Map<String, Object>> liste = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, Double> resultat = new HashMap<>();
            for (Map<String, Object> ligne : liste) {
                Object team = ligne.get("team"), moyenne = ligne.get("moyenneNote");
                if (team != null && moyenne != null) resultat.put((String) team, ((Number) moyenne).doubleValue());
            }
            return resultat;
        } catch (Exception e) {
            return Map.of();
        }
    }
}