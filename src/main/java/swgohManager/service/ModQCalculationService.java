package swgohManager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcul commun du ModQ (comptage des mods vitesse par tranche + formule),
 * utilisé par PlayerModQService (interne, avec historique) et ExternalPlayerModQService (externe, sans historique).
 */
@Service
@Slf4j
public class ModQCalculationService {

    private static final int ID_STAT_SECONDAIRE_VITESSE = 5; // unitStatId=5 = vitesse
    private static final double DIVISEUR_VALEUR = 100_000_000.0; // même échelle que unscaledDecimalValue

    /** Valeur pivot minimale nécessaire au comptage : idSecondaire + valeurSecondaire d'une ligne de mod. */
    public record ModSecondaryValue(Integer idSecondaire, Long valeurSecondaire) {}

    /** Répartition des mods vitesse par tranche. */
    public record ModSpeedCounts(int mod25Plus, int mod20_24, int mod15_19, int mod10_14) {}

    public ModSpeedCounts calculerRepartitionVitesse(List<ModSecondaryValue> mods) {
        int mod25Plus = 0, mod20_24 = 0, mod15_19 = 0, mod10_14 = 0;

        for (ModSecondaryValue mod : mods) {
            if (mod.idSecondaire() == null || mod.idSecondaire() != ID_STAT_SECONDAIRE_VITESSE) continue;
            if (mod.valeurSecondaire() == null) continue;

            double valeur = mod.valeurSecondaire() / DIVISEUR_VALEUR;
            if (valeur >= 25) mod25Plus++;
            else if (valeur >= 20) mod20_24++;
            else if (valeur >= 15) mod15_19++;
            else if (valeur >= 10) mod10_14++;
        }

        return new ModSpeedCounts(mod25Plus, mod20_24, mod15_19, mod10_14);
    }

    /** Formule ModQ : null si le GP personnage n'est pas disponible (cas externe sans scan de guilde réussi). */
    public Double calculerModQ(ModSpeedCounts counts, Long gpChar) {
        if (gpChar == null || gpChar <= 0) return null;
        return 100_000.0 * (0.8 * counts.mod15_19() + counts.mod20_24() + 1.2 * counts.mod25Plus()) / gpChar;
    }
}