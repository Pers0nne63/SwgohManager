package swgohManager.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.BaseIdLibelleProjection;
import swgohManager.controller.dto.DatacronDefinitionProjection;
import swgohManager.repository.DatacronAffixTemplateRepository;
import swgohManager.repository.UnitDefinitionRepository;

@Service
@RequiredArgsConstructor
public class DatacronDefinitionService {

    private static final Set<Integer> NIVEAUX_DTC = Set.of(3, 6, 9);

    private final DatacronAffixTemplateRepository repository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record StatDef(String libelle, String valeur) {}
    public record AffixeDef(Integer tier, String libelle, String description) {}
    public record FocusCard(String titre, List<StatDef> statsBrutes, List<AffixeDef> tiers) {}
    /** Une ligne de cards FDTC : tous les datacrons portant le même nom. */
    public record FocusGroupe(String nom, List<FocusCard> cartes) {}
    /** Une card DTC : un côté (niveau 3), une faction (niveau 6) ou une unité (niveau 9). */
    public record CarteDtc(String titre, List<AffixeDef> mecaniques) {}
    /** Une ligne de cards DTC : un niveau (3 / 6 / 9). */
    public record Niveau(Integer tier, String typeGroupe, List<CarteDtc> cartes) {}
    public record SetDefinitions(String setId, List<FocusGroupe> focus, List<Niveau> niveaux) {}

    public Map<String, SetDefinitions> construireParSet() {
        List<DatacronDefinitionProjection> lignes = repository.findDefinitions();

        Map<String, String> libellesParBaseId = unitDefinitionRepository.findDistinctBaseIdsAvecLibelle().stream()
                .collect(Collectors.toMap(
                        p -> p.getBaseId().toUpperCase(),
                        BaseIdLibelleProjection::getLibelle,
                        (a, b) -> a));

        Map<String, List<DatacronDefinitionProjection>> parSet = lignes.stream()
                .filter(l -> l.getSet() != null)
                .collect(Collectors.groupingBy(DatacronDefinitionProjection::getSet));

        Map<String, SetDefinitions> resultat = new LinkedHashMap<>();

        parSet.forEach((setId, rows) -> resultat.put(setId, new SetDefinitions(
                setId,
                construireFocus(rows, libellesParBaseId),
                construireNiveaux(rows, libellesParBaseId))));

        return resultat;
    }

    // =====================================================================
    // FDTC : une ligne par nom de datacron
    // =====================================================================
    private List<FocusGroupe> construireFocus(List<DatacronDefinitionProjection> rows,
                                              Map<String, String> libellesParBaseId) {

        Map<String, List<DatacronDefinitionProjection>> focusParTemplate = rows.stream()
                .filter(r -> Boolean.TRUE.equals(r.getFocused()))
                .collect(Collectors.groupingBy(DatacronDefinitionProjection::getIdTemplate,
                        LinkedHashMap::new, Collectors.toList()));

        List<FocusCard> cartes = focusParTemplate.entrySet().stream()
                .map(e -> new FocusCard(
                        e.getValue().stream()
                                .map(DatacronDefinitionProjection::getFocusLibelle)
                                .filter(fl -> fl != null && !fl.isBlank())
                                .findFirst()
                                .map(fl -> libellesParBaseId.getOrDefault(fl.toUpperCase(), fl))
                                .orElse(e.getKey()),
                        sommerStats(e.getValue()),
                        e.getValue().stream()
                                .filter(r -> r.getAbilityId() != null && r.getTier() != null)
                                .map(r -> new AffixeDef(r.getTier(), r.getLibelle(), r.getDescription()))
                                .distinct()
                                .sorted(Comparator.comparing(AffixeDef::tier)
                                        .thenComparing(a -> a.description() == null ? "" : a.description()))
                                .toList()))
                // doublons stricts (même titre, mêmes stats, mêmes affixes)
                .collect(Collectors.toMap(this::signature, c -> c, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .toList();

        // Regroupement par nom : les datacrons de même nom partagent la même ligne
        Map<String, List<FocusCard>> parNom = cartes.stream()
                .collect(Collectors.groupingBy(FocusCard::titre,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                        Collectors.toList()));

        return parNom.entrySet().stream()
                .map(e -> new FocusGroupe(e.getKey(), e.getValue()))
                .toList();
    }

    // =====================================================================
    // DTC : une ligne par niveau, une card par côté / faction / unité
    // =====================================================================
    private List<Niveau> construireNiveaux(List<DatacronDefinitionProjection> rows,
                                           Map<String, String> libellesParBaseId) {

        // niveau -> nom de la card -> (abilityId -> mécanique)
        Map<Integer, Map<String, Map<String, AffixeDef>>> parNiveau = new TreeMap<>();

        rows.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getFocused()))
                .filter(r -> r.getAbilityId() != null && r.getTier() != null && NIVEAUX_DTC.contains(r.getTier()))
                .forEach(r -> {
                    String nomCard = nomCard(r, libellesParBaseId);
                    parNiveau
                            .computeIfAbsent(r.getTier(), k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                            .computeIfAbsent(nomCard, k -> new LinkedHashMap<>())
                            .putIfAbsent(r.getAbilityId(),
                                    new AffixeDef(r.getTier(), r.getLibelle(), r.getDescription()));
                });

        return parNiveau.entrySet().stream()
                .map(niv -> new Niveau(
                        niv.getKey(),
                        typeGroupe(niv.getKey()),
                        niv.getValue().entrySet().stream()
                                .map(card -> new CarteDtc(
                                        card.getKey(),
                                        card.getValue().values().stream()
                                                .sorted(Comparator.comparing(
                                                        (AffixeDef a) -> a.description() == null ? "" : a.description().toLowerCase()))
                                                .toList()))
                                .toList()))
                .toList();
    }

    private String typeGroupe(int tier) {
        return switch (tier) {
            case 3 -> "côté";
            case 6 -> "faction";
            default -> "unité";
        };
    }

    private String nomCard(DatacronDefinitionProjection r, Map<String, String> libellesParBaseId) {
        String target = r.getTarget();
        String libelle = r.getLibelle();

        switch (r.getTier()) {
            case 3:
                if ("lightside".equalsIgnoreCase(target)) return "Côté Lumineux";
                if ("darkside".equalsIgnoreCase(target)) return "Côté Obscur";
                return premierNonVide(libelle, target);
            case 9:
                if (target != null && libellesParBaseId.containsKey(target.toUpperCase())) {
                    return libellesParBaseId.get(target.toUpperCase());
                }
                return premierNonVide(libelle, target);
            default: // 6 : faction
                return premierNonVide(libelle, target);
        }
    }

    private String premierNonVide(String... valeurs) {
        for (String v : valeurs) {
            if (v != null && !v.isBlank()) return v;
        }
        return "Autre";
    }

    // =====================================================================
    // Utilitaires
    // =====================================================================
    private List<StatDef> sommerStats(List<DatacronDefinitionProjection> rows) {
        Map<String, BigDecimal[]> sommes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (DatacronDefinitionProjection r : rows) {
            if (r.getStatType() == null) continue;

            String cle = r.getStatLibelle() != null ? r.getStatLibelle() : r.getStatType();
            BigDecimal min = r.getStatMin() != null ? r.getStatMin() : BigDecimal.ZERO;
            BigDecimal max = r.getStatMax() != null ? r.getStatMax() : min;

            BigDecimal[] s = sommes.computeIfAbsent(cle, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            s[0] = s[0].add(min);
            s[1] = s[1].add(max);
        }

        return sommes.entrySet().stream()
                .map(e -> new StatDef(e.getKey(), formaterPlage(e.getValue()[0], e.getValue()[1])))
                .toList();
    }

    private String signature(FocusCard c) {
        String stats = c.statsBrutes().stream()
                .map(s -> s.libelle() + "=" + s.valeur())
                .distinct().sorted()
                .collect(Collectors.joining(";"));
        String tiers = c.tiers().stream()
                .map(t -> t.tier() + ":" + t.description())
                .distinct().sorted()
                .collect(Collectors.joining(";"));
        return c.titre() + "|" + stats + "|" + tiers;
    }

    private String formaterPlage(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return "-";
        if (min == null) return max.toPlainString();
        if (max == null || min.compareTo(max) == 0) return min.toPlainString();
        return min.toPlainString() + " – " + max.toPlainString();
    }
}