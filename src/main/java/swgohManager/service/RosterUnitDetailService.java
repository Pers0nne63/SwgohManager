package swgohManager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.model.RosterUnitStatActuel;
import swgohManager.repository.RosterUnitModActuelRepository;
import swgohManager.repository.RosterUnitStatActuelRepository;

/**
 * Détail d'une unité pour l'onglet Roster : stats calculées + mods équipés (6 emplacements).
 */
@Service
@RequiredArgsConstructor
public class RosterUnitDetailService {

    /** Les valeurs de mods sont stockées × 100 000 000 (même échelle que le graphique de vitesse). */
    private static final double ECHELLE_MOD = 100_000_000.0;

    /**
     * Les stats pourcentage de roster_unit_stat_actuel sont-elles stockées en fraction (0,25 = 25 %) ?
     * Si l'affichage est 100 fois trop petit, laisser 100 ; s'il est 100 fois trop grand, mettre 1.
     */
    private static final double FACTEUR_POURCENT_STATS = 1.0;

    private static final String[] FORMES = { "square", "arrow", "diamond", "triangle", "circle", "cross" };

    private record StatDef(String label, boolean pourcent) {}

    /** Identifiants de stats des mods (identiques à ceux du jeu). */
    private static final Map<Integer, StatDef> STATS_MOD = Map.ofEntries(
            Map.entry(1, new StatDef("Santé", false)),
            Map.entry(5, new StatDef("Vitesse", false)),
            Map.entry(16, new StatDef("Dégâts critiques", true)),
            Map.entry(17, new StatDef("Pouvoir", true)),
            Map.entry(18, new StatDef("Ténacité", true)),
            Map.entry(28, new StatDef("Protection", false)),
            Map.entry(41, new StatDef("Attaque", false)),
            Map.entry(42, new StatDef("Défense", false)),
            Map.entry(48, new StatDef("Attaque %", true)),
            Map.entry(49, new StatDef("Défense %", true)),
            Map.entry(52, new StatDef("Précision", true)),
            Map.entry(53, new StatDef("Chance de coup critique", true)),
            Map.entry(54, new StatDef("Évitement des critiques", true)),
            Map.entry(55, new StatDef("Santé %", true)),
            Map.entry(56, new StatDef("Protection %", true)));

    /** Sets de mods : 1 à 8 → libellé complet et libellé court. */
    private static final String[] SETS_LIBELLE = { "", "Santé", "Attaque", "Défense", "Vitesse",
            "Chance de coup critique", "Dégâts critiques", "Pouvoir", "Ténacité" };
    private static final String[] SETS_COURT = { "", "SAN", "ATQ", "DÉF", "VIT", "CC", "DC", "POU", "TÉN" };
    
 // Map pour associer chaque ID de stat à son fichier d'icône correspondant dans /img/game/stat/
    private static final Map<Integer, String> STAT_ICONS = Map.ofEntries(
            Map.entry(1, "icon_stat_maxhealth.png"),       // Santé
            Map.entry(5, "icon_stat_speed.png"),         // Vitesse (ou utilisez l'icône de vitesse dédiée si extraite)
            Map.entry(16, "icon_stat_criticaldamage.png"), // Dégâts critiques
            Map.entry(17, "icon_stat_potency.png"),     // Pouvoir
            Map.entry(18, "icon_stat_tenacity.png"),     // Ténacité
            Map.entry(28, "icon_stat_protection.png"),    // Protection
            Map.entry(41, "icon_stat_damage.png"),       // Attaque
            Map.entry(42, "icon_stat_armor.png"),        // Défense
            Map.entry(48, "icon_stat_damage.png"),       // Attaque %
            Map.entry(49, "icon_stat_armor.png"),        // Défense %
            Map.entry(52, "icon_stat_accuracy.png"),     // Précision
            Map.entry(53, "icon_stat_criticalchance.png"), // Chance de coup critique
            Map.entry(54, "icon_stat_criticalavoidance.png"), // Évitement des critiques
            Map.entry(55, "icon_stat_maxhealth.png"),    // Santé %
            Map.entry(56, "icon_stat_protection.png")     // Protection %
    );
    
 // Association ID du Set (1 à 8) -> Fichier de l'icône du Set
    private static final Map<String, String> SET_ICONS = Map.of(
            "1", "icon_stat_maxhealth.png",    // 1: Santé (Croix)
            "2", "icon_stat_damage.png",      // 2: Attaque (Déflagration)
            "3", "icon_stat_armor.png",      // 3: Défense (Bouclier)
            "4", "icon_stat_speed.png",        // 4: Vitesse (Runner)
            "5", "icon_stat_criticalchance.png",   // 5: Chance de CC
            "6", "icon_stat_criticaldamage.png",   // 6: Dégâts Critiques
            "7", "icon_stat_potency.png",      // 7: Pouvoir
            "8", "icon_stat_tenacity.png"      // 8: Ténacité
    );

    public record StatLigne(String label, String valeur) {}

    public record StatModLigne(int statId, String label, String valeur, boolean vitesse, Integer rollSecondaire, String iconFileName) {}

    public record ModVm(boolean vide, String forme, String set, String setLibelle, String setCourt,
            int rarete, int niveau, int tier, String primaireIconFileName,
            StatModLigne primaire, List<StatModLigne> secondaires) {
    	static ModVm vide(String forme) {
    		return new ModVm(true, forme, null, null, null, 0, 0, 0, null, null, List.of());
    	}
    }

    /** mods contient toujours 6 éléments (emplacements 1 à 6, vides si pas de mod équipé). */
    public record UnitDetail(List<StatLigne> stats, List<ModVm> mods) {}

    private final RosterUnitStatActuelRepository rosterUnitStatActuelRepository;
    private final RosterUnitModActuelRepository rosterUnitModActuelRepository;

    public UnitDetail construire(String playerId, String idUnit) {
        List<StatLigne> stats = rosterUnitStatActuelRepository.findByPlayerIdAndIdUnit(playerId, idUnit)
                .map(this::construireStats)
                .orElse(List.of());

        List<RosterUnitModActuel> lignes = rosterUnitModActuelRepository.findByPlayerIdAndIdUnit(playerId, idUnit);
        Map<String, List<RosterUnitModActuel>> parMod = lignes.stream()
                .filter(l -> l.getIdMod() != null)
                .collect(Collectors.groupingBy(RosterUnitModActuel::getIdMod, LinkedHashMap::new, Collectors.toList()));

        Map<String, ModVm> parPosition = new HashMap<>();
        for (List<RosterUnitModActuel> modLignes : parMod.values()) {
            parPosition.put(modLignes.get(0).getPosition(), construireMod(modLignes));
        }

        List<ModVm> slots = new ArrayList<>();
        for (int p = 1; p <= 6; p++) {
            ModVm mod = parPosition.get(String.valueOf(p));
            slots.add(mod != null ? mod : ModVm.vide(FORMES[p - 1]));
        }
        return new UnitDetail(stats, slots);
    }

    private ModVm construireMod(List<RosterUnitModActuel> lignes) {
        RosterUnitModActuel premiere = lignes.get(0);
        int position = parseEntier(premiere.getPosition(), 1);
        String forme = FORMES[Math.min(Math.max(position, 1), 6) - 1];
        int set = parseEntier(premiere.getSet(), 0);
        boolean setConnu = set >= 1 && set <= 8;

        StatModLigne primaire = (premiere.getIdPrimaire() != null && premiere.getValeurPrimaire() != null)
                ? ligneMod(premiere.getIdPrimaire(), premiere.getValeurPrimaire(), false) : null;

        // On prend l'icône liée au SET du mod (et non à sa stat primaire) !
        String setIconFileName = SET_ICONS.getOrDefault(String.valueOf(set), "icon_stat_health.png");

        List<StatModLigne> secondaires = lignes.stream()
                .filter(l -> l.getIdSecondaire() != null && l.getValeurSecondaire() != null)
                .sorted(Comparator.comparingInt((RosterUnitModActuel l) ->
                        l.getOrdreSecondaire() != null ? l.getOrdreSecondaire() : 99))
                .map(l -> {
                    StatModLigne baseLigne = ligneMod(l.getIdSecondaire(), l.getValeurSecondaire(), true);
                    return new StatModLigne(baseLigne.statId(), baseLigne.label(), baseLigne.valeur(), baseLigne.vitesse(), l.getRollSecondaire(), baseLigne.iconFileName());
                })
                .filter(Objects::nonNull)
                .toList();

        return new ModVm(false, forme, String.valueOf(set),
                setConnu ? SETS_LIBELLE[set] : "Set inconnu",
                setConnu ? SETS_COURT[set] : "?",
                parseEntier(premiere.getRarity(), 0),
                premiere.getNiveau() != null ? premiere.getNiveau() : 0,
                premiere.getTier() != null ? premiere.getTier() : 1,
                setIconFileName, // Transmet l'icône du SET
                primaire, secondaires);
    }

    private StatModLigne ligneMod(int statId, long brut, boolean secondaire) {
        StatDef def = STATS_MOD.getOrDefault(statId, new StatDef("Stat " + statId, false));
        double v = brut / ECHELLE_MOD;
        String valeur = def.pourcent()
                ? String.format(Locale.FRANCE, "+%.2f %%", v * 100)
                : String.format(Locale.FRANCE, "+%,.0f", v);
        String iconFile = STAT_ICONS.getOrDefault(statId, "icon_stat_damage.png");
        return new StatModLigne(statId, def.label(), valeur, statId == 5, null, iconFile);
    }

    private List<StatLigne> construireStats(RosterUnitStatActuel s) {
        List<StatLigne> l = new ArrayList<>();
        ajouter(l, "Santé", s.getSante(), false);
        ajouter(l, "Protection", s.getProtection(), false);
        ajouter(l, "Vitesse", s.getVitesse(), false);
        ajouter(l, "Attaque physique", s.getAttaquePhysique(), false);
        ajouter(l, "Attaque spéciale", s.getAttaqueSpeciale(), false);
        ajouter(l, "Armure", s.getArmure(), true);
        ajouter(l, "Résistance", s.getResistance(), true);
        ajouter(l, "Pénétration d'armure", s.getPenetrationArmure(), false);
        ajouter(l, "Pénétration de résistance", s.getPenetrationResistance(), false);
        ajouter(l, "Esquive", s.getEsquive(), true);
        ajouter(l, "Déviation", s.getDeviation(), true);
        ajouter(l, "Coup critique physique", s.getCcPhysique(), true);
        ajouter(l, "Coup critique spécial", s.getCcSpeciaux(), true);
        ajouter(l, "Dégâts critiques", s.getDegatsCritiques(), true);
        ajouter(l, "Pouvoir", s.getPouvoir(), true);
        ajouter(l, "Ténacité", s.getTenacite(), true);
        ajouter(l, "Vol de santé", s.getVolDeSante(), true);
        //ajouter(l, "Précision physique", s.getPrecisionPhysique(), true);
        //ajouter(l, "Précision spéciale", s.getPrecisionSpeciale(), true);
        //ajouter(l, "Esquive critique physique", s.getEsquiveCritiquePhysique(), true);
        //ajouter(l, "Esquive critique spéciale", s.getEsquiveCritiqueSpeciale(), true);
        return l;
    }

    private static void ajouter(List<StatLigne> lignes, String label, Double valeur, boolean pourcent) {
        if (valeur == null || valeur == 0.0) return;
        String texte = pourcent
                ? String.format(Locale.FRANCE, "%.2f %%", valeur * FACTEUR_POURCENT_STATS)
                : String.format(Locale.FRANCE, "%,.0f", valeur);
        lignes.add(new StatLigne(label, texte));
    }

    private static int parseEntier(String s, int defaut) {
        if (s == null || s.isBlank()) return defaut;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }
}