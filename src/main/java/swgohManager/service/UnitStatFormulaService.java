package swgohManager.service;

import swgohManager.util.ModAggregationUtil.ModAccumulator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnitStatFormulaService {

    public record UnitStatResult(
            double sante, double protection, double vitesse, double attaquePhysique, double attaqueSpeciale,
            double armure, double resistance, double penetrationArmure, double penetrationResistance,
            double esquive, double deviation, double ccPhysique, double ccSpeciaux, double degatsCritiques,
            double pouvoir, double tenacite, double volDeSante, double precisionPhysique, double precisionSpeciale,
            double esquiveCritiquePhysique, double esquiveCritiqueSpeciale, double defense
    ) {}

    public double[] calculerStatsDeBase(Map<Integer, Double> unitStats, Map<Integer, Double> statGrowth,
                                         Map<Integer, Double> statRelicDefinition, Map<Integer, Double> relicGrowth,
                                         Map<Integer, Double> masteryStat, int level, String primaryStat) {
        double[] s = new double[62];

        s[61] = Math.floor(g(unitStats, 61) + g(statRelicDefinition, 61) + g(relicGrowth, 61));
        s[2] = Math.floor(g(unitStats, 2) + level * (g(statGrowth, 2) + g(relicGrowth, 2)));
        s[3] = Math.floor(g(unitStats, 3) + level * (g(statGrowth, 3) + g(relicGrowth, 3)));
        s[4] = Math.floor(g(unitStats, 4) + level * (g(statGrowth, 4) + g(relicGrowth, 4)));

        for (int i = 5; i <= 60; i++) {
            s[i] = g(unitStats, i) + s[61] * g(masteryStat, i) + g(statRelicDefinition, i);
        }

        s[1] = Math.floor(g(unitStats, 1) + 18 * s[2] + g(statRelicDefinition, 1) + s[61] * g(masteryStat, 1));

        double mainStatBonus = switch (primaryStat) {
            case "STR" -> 1.4 * s[2];
            case "AGI" -> 1.4 * s[3];
            case "TAC" -> 1.4 * s[4];
            default -> 0;
        };
        s[6] += Math.floor(mainStatBonus);
        s[7] += Math.floor(2.4 * s[4]);

        double armorRaw = g(unitStats, 8) + 0.14 * s[2] + 0.07 * s[3];
        s[8] = round2(100 * (armorRaw / (7.5 * level + armorRaw)) * (1 + s[61] * g(masteryStat, 8) / 100));

        double resistanceRaw = g(unitStats, 9) + 0.1 * s[4];
        s[9] = round2(100 * (resistanceRaw / (7.5 * level + resistanceRaw)) * (1 + s[61] * g(masteryStat, 9) / 100));

        s[14] = round2(100 * (0.1 + (g(unitStats, 14) + 0.4 * s[3]) / 2400) + s[61] * g(masteryStat, 14));
        s[15] = round2(100 * (0.1 + (g(unitStats, 15)) / 2400) + s[61] * g(masteryStat, 15));

        s[27] = g(unitStats, 27) * 100 + s[61] * g(masteryStat, 27) + 100 * g(statRelicDefinition, 27);

        return s;
    }

    public UnitStatResult calculerFinal(double[] s, int level, ModAccumulator mod) {
        double defensePhys = (level * 7.5 * s[8]) / (100 - s[8]);
        double defenseSpe = (level * 7.5 * s[9]) / (100 - s[9]);

        double modDefensePhys = defensePhys * (1 + mod.pDef / 100) + mod.fDef;
        double armureFinal = 100 * modDefensePhys / (level * 7.5 + modDefensePhys);
        double modDefenseSpe = defenseSpe * (1 + mod.pDef / 100) + mod.fDef;
        double resistanceFinal = 100 * modDefenseSpe / (level * 7.5 + modDefenseSpe);
        double defenseFinal = (armureFinal * (level * 7.5)) / (100 - armureFinal);

        double dcBase = s[16] + 150;
        double pouvoirBase = s[17] * 100;
        double tenaciteBase = s[18] * 100 + 15;

        return new UnitStatResult(
                s[1] * (1 + mod.pSante / 100) + mod.fSante,
                s[28] * (1 + mod.pProt / 100) + mod.fProt,
                s[5] * (1 + mod.pSpeed / 100) + mod.speed,
                s[6] * (1 + mod.pOff / 100) + mod.fOff,
                s[7] * (1 + mod.pOff / 100) + mod.fOff,
                armureFinal,
                resistanceFinal,
                s[10],
                s[11],
                s[12] + 2,
                s[13] + 2,
                s[14] + mod.cc,
                s[15] + mod.cc,
                dcBase + mod.dc,
                pouvoirBase + mod.pot,
                tenaciteBase + mod.ten,
                s[27],
                s[37] + mod.acc,
                s[38] + mod.acc,
                s[39] + mod.critAvoid,
                s[40] + mod.critAvoid,
                defenseFinal
        );
    }

    private double g(Map<Integer, Double> map, int key) {
        return map.getOrDefault(key, 0.0);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}