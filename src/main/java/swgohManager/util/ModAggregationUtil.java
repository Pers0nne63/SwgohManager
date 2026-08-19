package swgohManager.util;

import swgohManager.model.ModLigne;
import java.util.*;
import java.util.stream.Collectors;

public class ModAggregationUtil {

    public static class ModAccumulator {
        public double speed, pSpeed, pOff, fOff, pSante, fSante, pProt, fProt, pDef, fDef;
        public double pot, ten, cc, dc, critAvoid, acc;
    }

    public static ModAccumulator agregerMods(List<? extends ModLigne> modsUnite, Map<Integer, Boolean> isDecimalByStat) {
        ModAccumulator acc = new ModAccumulator();
        int[] setCounts = new int[9];

        Map<String, List<ModLigne>> parMod = modsUnite.stream()
                .collect(Collectors.groupingBy(ModLigne::getIdMod));

        for (List<ModLigne> groupe : parMod.values()) {
            ModLigne premiere = groupe.get(0);

            if (premiere.getSet() != null) {
                try {
                    setCounts[Integer.parseInt(premiere.getSet())]++;
                } catch (NumberFormatException ignored) {}
            }

            if (premiere.getIdPrimaire() != null && premiere.getValeurPrimaire() != null) {
                appliquerStatMod(acc, premiere.getIdPrimaire(), premiere.getValeurPrimaire(),
                        isDecimalByStat.getOrDefault(premiere.getIdPrimaire(), false));
            }

            for (ModLigne ligne : groupe) {
                if (ligne.getIdSecondaire() != null && ligne.getValeurSecondaire() != null) {
                    appliquerStatMod(acc, ligne.getIdSecondaire(), ligne.getValeurSecondaire(),
                            isDecimalByStat.getOrDefault(ligne.getIdSecondaire(), false));
                }
            }
        }

        if (setCounts[2] >= 4) acc.pOff += 15;
        if (setCounts[4] >= 4) acc.pSpeed = 10;
        if (setCounts[6] >= 4) acc.dc += 30;
        if (setCounts[1] == 2) acc.pSante += 10; if (setCounts[1] == 4) acc.pSante += 20; if (setCounts[1] == 6) acc.pSante += 30;
        if (setCounts[3] == 2) acc.pDef += 25; if (setCounts[3] == 4) acc.pDef += 50; if (setCounts[3] == 6) acc.pDef += 75;
        if (setCounts[5] == 2) acc.cc += 8; if (setCounts[5] == 4) acc.cc += 16; if (setCounts[5] == 6) acc.cc += 24;
        if (setCounts[8] == 2) acc.ten += 20; if (setCounts[8] == 4) acc.ten += 40; if (setCounts[8] == 6) acc.ten += 60;
        if (setCounts[7] == 2) acc.pot += 15; if (setCounts[7] == 4) acc.pot += 30; if (setCounts[7] == 6) acc.pot += 45;

        return acc;
    }

    private static void appliquerStatMod(ModAccumulator acc, int statId, long valeur, boolean isDecimal) {
        double v = isDecimal ? valeur / 1_000_000.0 : valeur / 100_000_000.0;
        switch (statId) {
            case 1 -> acc.fSante += v;
            case 55 -> acc.pSante += v;
            case 16 -> acc.dc += v;
            case 17 -> acc.pot += v;
            case 18 -> acc.ten += v;
            case 52 -> acc.acc += v;
            case 53 -> acc.cc += v;
            case 54 -> acc.critAvoid += v;
            case 5 -> acc.speed += v;
            case 41 -> acc.fOff += v;
            case 48 -> acc.pOff += v;
            case 42 -> acc.fDef += v;
            case 49 -> acc.pDef += v;
            case 28 -> acc.fProt += v;
            case 56 -> acc.pProt += v;
            default -> { }
        }
    }
}