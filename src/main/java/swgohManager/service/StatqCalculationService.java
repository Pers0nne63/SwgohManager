package swgohManager.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import swgohManager.model.UnitStatPriority;
import swgohManager.model.UnitStatValues;
import swgohManager.repository.UnitStatPriorityRepository;

import java.util.*;

/**
 * Calcul commun du score StatQ pour un joueur (interne ou externe), à partir de ses stats
 * actuelles/objectif déjà persistées. Ne connaît ni l'entité de destination ni l'idSync.
 */
@Service
@RequiredArgsConstructor
public class StatqCalculationService {

    private final UnitStatPriorityRepository unitStatPriorityRepository;

    public record StatCible(String baseId, String team, int statId) {}
    public record DetailLigne(String baseId, String team, int statId, Double valeurActuelle, Double valeurObjectif, Double variation, int note) {}
    public record StatqResultat(double statq, int nbStats, List<DetailLigne> details) {}

    public List<StatCible> chargerCibles() {
        return aplatirCibles(unitStatPriorityRepository.findAll());
    }

    private List<StatCible> aplatirCibles(List<UnitStatPriority> priorites) {
        List<StatCible> cibles = new ArrayList<>();
        for (UnitStatPriority p : priorites) {
            ajouterSiPresent(cibles, p, p.getStatId1());
            ajouterSiPresent(cibles, p, p.getStatId2());
            ajouterSiPresent(cibles, p, p.getStatId3());
            ajouterSiPresent(cibles, p, p.getStatId4());
        }
        return cibles;
    }

    private void ajouterSiPresent(List<StatCible> cibles, UnitStatPriority p, Integer statId) {
        if (statId != null) cibles.add(new StatCible(p.getBaseId(), p.getTeam(), statId));
    }

    /**
     * Calcule le score StatQ d'un joueur.
     * @param idUnitParBaseId mapping baseId -> idUnit pour CE joueur uniquement
     * @param statsActuelParIdUnit stats actuelles de ce joueur, indexées par idUnit
     * @param statsObjectifParIdUnit stats objectif de ce joueur, indexées par idUnit
     */
    public StatqResultat calculerPourJoueur(List<StatCible> cibles,
                                             Map<String, String> idUnitParBaseId,
                                             Map<String, UnitStatValues> statsActuelParIdUnit,
                                             Map<String, UnitStatValues> statsObjectifParIdUnit) {
        List<Integer> notes = new ArrayList<>();
        List<DetailLigne> details = new ArrayList<>();

        for (StatCible cible : cibles) {
            String idUnit = idUnitParBaseId.get(cible.baseId());

            UnitStatValues statsActuel = idUnit != null ? statsActuelParIdUnit.get(idUnit) : null;
            UnitStatValues statsObjectif = idUnit != null ? statsObjectifParIdUnit.get(idUnit) : null;

            Double valActuelle = extraireStat(statsActuel, cible.statId());
            Double valObjectif = extraireStat(statsObjectif, cible.statId());

            Double variation = null;
            int note;
            if (valActuelle == null || valObjectif == null || valObjectif == 0) {
                note = 0;
            } else {
                variation = (valActuelle - valObjectif) / valObjectif;
                note = attribuerNote(variation);
            }

            notes.add(note);
            details.add(new DetailLigne(cible.baseId(), cible.team(), cible.statId(), valActuelle, valObjectif, variation, note));
        }

        double statq = notes.stream().mapToInt(Integer::intValue).sum();
        return new StatqResultat(statq, notes.size(), details);
    }

    private Double extraireStat(UnitStatValues s, int statId) {
        if (s == null) return null;
        return switch (statId) {
            case 1 -> s.getSante();
            case 5 -> s.getVitesse();
            case 6 -> s.getAttaquePhysique();
            case 7 -> s.getAttaqueSpeciale();
            case 8 -> s.getArmure();
            case 9 -> s.getResistance();
            case 14 -> s.getCcPhysique();
            case 15 -> s.getCcSpeciaux();
            case 17 -> s.getPouvoir();
            case 18 -> s.getTenacite();
            case 28 -> s.getProtection();
            default -> null;
        };
    }

    private int attribuerNote(double variation) {
        if (variation > 0) return 4;
        if (variation > -0.025) return 3;
        if (variation > -0.05) return 2;
        if (variation > -0.075) return 1;
        return 0;
    }
}