package swgohManager.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.RosterProgressionJourProjection;
import swgohManager.repository.RosterUnitProgressionRepository;

@Service
@RequiredArgsConstructor
public class ActiviteRosterService {

    private static final int NB_JOURS_PAR_DEFAUT = 7;

    private final RosterUnitProgressionRepository progressionRepository;

    public record ActiviteLigneVM(
            String baseId,
            String libelle,
            boolean nouvelleUnite,
            boolean omicronObtenu,
            Integer etoilesAvant, Integer etoilesApres,
            Integer gearAvant, Integer gearApres,
            Integer relicAvant, Integer relicApres) {}

    public record ActiviteJoueurVM(String playerId, String playerName, List<ActiviteLigneVM> lignes) {}
    public record ActiviteJourVM(LocalDate jour, List<ActiviteJoueurVM> joueurs) {}
    public record ActiviteJourUniteVM(LocalDate jour, List<ActiviteLigneVM> lignes) {}

    private record ActiviteJoueurAccumulateur(String playerName, List<ActiviteLigneVM> lignes) {
        ActiviteJoueurAccumulateur(String playerName) {
            this(playerName, new ArrayList<>());
        }
    }

    public List<ActiviteJourVM> getActiviteRecente() {
        Instant fin = Instant.now();
        Instant debut = fin.minus(NB_JOURS_PAR_DEFAUT, ChronoUnit.DAYS);

        List<RosterProgressionJourProjection> lignesBrutes = progressionRepository.findProgressionParJour(debut, fin);

        Map<LocalDate, Map<String, ActiviteJoueurAccumulateur>> parJour = new LinkedHashMap<>();

        for (RosterProgressionJourProjection p : lignesBrutes) {
            String nomAffiche = p.getPlayerName() != null ? p.getPlayerName() : p.getPlayerId();

            parJour
                    .computeIfAbsent(p.getJour(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(p.getPlayerId(), k -> new ActiviteJoueurAccumulateur(nomAffiche))
                    .lignes()
                    .add(construireLigne(p));
        }

        return parJour.entrySet().stream()
                .map(e -> new ActiviteJourVM(
                        e.getKey(),
                        e.getValue().entrySet().stream()
                                .map(entry -> new ActiviteJoueurVM(
                                        entry.getKey(), entry.getValue().playerName(), entry.getValue().lignes()))
                                .toList()))
                .toList();
    }

    public List<ActiviteJourUniteVM> getActiviteJoueur(String playerId, int nbJours) {
        Instant fin = Instant.now();
        Instant debut = fin.minus(nbJours, ChronoUnit.DAYS);

        List<RosterProgressionJourProjection> lignesBrutes =
                progressionRepository.findProgressionParJourPourJoueur(playerId, debut, fin);

        Map<LocalDate, List<ActiviteLigneVM>> parJour = new LinkedHashMap<>();

        for (RosterProgressionJourProjection p : lignesBrutes) {
            parJour.computeIfAbsent(p.getJour(), k -> new ArrayList<>()).add(construireLigne(p));
        }

        return parJour.entrySet().stream()
                .map(e -> new ActiviteJourUniteVM(e.getKey(), e.getValue()))
                .toList();
    }

    private ActiviteLigneVM construireLigne(RosterProgressionJourProjection p) {
        return new ActiviteLigneVM(
                p.getBaseId(),
                p.getLibelle(),
                Boolean.TRUE.equals(p.getNouvelleUnite()),
                Boolean.TRUE.equals(p.getOmicronObtenu()),
                p.getEtoilesAvant(), p.getEtoilesApres(),
                p.getGearAvant(), p.getGearApres(),
                p.getRelicAvant(), p.getRelicApres());
    }
}