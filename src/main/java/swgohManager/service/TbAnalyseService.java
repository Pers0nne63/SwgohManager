package swgohManager.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.TbRoundPlayerStatsProjection;
import swgohManager.model.Joueur;
import swgohManager.model.TbPlanRound;
import swgohManager.model.TbPlaneteReference;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.TbPlanRoundRepository;
import swgohManager.repository.TbPlaneteReferenceRepository;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.repository.TerritoryBattleRepository;

@Service
@RequiredArgsConstructor
public class TbAnalyseService {

    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbPlanRoundRepository tbPlanRoundRepository;
    private final TbPlaneteReferenceRepository tbPlaneteReferenceRepository;
    private final TbScoreJoueurRepository tbScoreJoueurRepository;
    private final JoueurRepository joueurRepository;

    public record RoundDetail(
            int roundNum,
            long pgDeployee,
            long pointsCombat, Long maxPointsCombat, Double pctPointsCombat,
            long combats, Integer maxCombats, Double pctCombats,
            long vagues, Integer maxVagues, Double pctVagues,
            long ms, Integer maxMs, Double pctMs
    ) {}

    public record PlayerAnalyse(String playerId, String playerName, List<RoundDetail> rounds) {}

    private record RoundMax(long maxCombat, int maxCombats, int maxVagues, int maxMs) {}

    public List<PlayerAnalyse> analyserTb(Long tbId) {
        TerritoryBattle tb = territoryBattleRepository.findById(tbId).orElse(null);
        if (tb == null) return List.of();

        Map<Integer, RoundMax> maxParRound = (tb.getPlanId() != null)
                ? calculerMaxParRound(tb.getPlanId())
                : Map.of();

        Map<String, String> nomJoueurs = joueurRepository.findAllByPresentInGuildTrue().stream()
                .collect(Collectors.toMap(Joueur::getPlayerId, Joueur::getPlayerName));

        Map<String, List<RoundDetail>> parJoueur = new LinkedHashMap<>();

        for (int round = 1; round <= 6; round++) {
            RoundMax max = maxParRound.get(round);
            List<TbRoundPlayerStatsProjection> lignes = tbScoreJoueurRepository.findStatsParRoundEtTb(tbId, round);

            for (TbRoundPlayerStatsProjection l : lignes) {
                long pg = nz(l.getPower());
                long summary = nz(l.getSummary());
                long pointsCombat = summary - pg;
                long combats = nz(l.getStrikeAttempt());
                long vagues = nz(l.getStrikeEncounter());
                long ms = nz(l.getCovertAttempt());

                Long maxCombat = max != null ? max.maxCombat() : null;
                Integer maxCombats = max != null ? max.maxCombats() : null;
                Integer maxVagues = max != null ? max.maxVagues() : null;
                Integer maxMs = max != null ? max.maxMs() : null;

                RoundDetail detail = new RoundDetail(
                        round, pg,
                        pointsCombat, maxCombat, pourcentageL(pointsCombat, maxCombat),
                        combats, maxCombats, pourcentageI(combats, maxCombats),
                        vagues, maxVagues, pourcentageI(vagues, maxVagues),
                        ms, maxMs, pourcentageI(ms, maxMs)
                );

                parJoueur.computeIfAbsent(l.getPlayerId(), k -> new ArrayList<>()).add(detail);
            }
        }

        List<PlayerAnalyse> resultat = new ArrayList<>();
        for (Map.Entry<String, List<RoundDetail>> entry : parJoueur.entrySet()) {
            String nom = nomJoueurs.getOrDefault(entry.getKey(), entry.getKey());
            resultat.add(new PlayerAnalyse(entry.getKey(), nom, entry.getValue()));
        }
        resultat.sort(Comparator.comparing(PlayerAnalyse::playerName, String.CASE_INSENSITIVE_ORDER));
        return resultat;
    }

    public List<RoundDetail> obtenirSyntheseRounds(Long tbId) {
        List<PlayerAnalyse> analyseJoueurs = analyserTb(tbId);
        return calculerSyntheseRounds(analyseJoueurs);
    }

    public List<RoundDetail> calculerSyntheseRounds(List<PlayerAnalyse> analyseJoueurs) {
        if (analyseJoueurs == null || analyseJoueurs.isEmpty()) {
            return List.of();
        }

        List<RoundDetail> synthese = new ArrayList<>();

        for (int round = 1; round <= 6; round++) {
            final int rNum = round;

            long pgDeployee = 0;
            long pointsCombat = 0;
            Long maxPointsCombat = null;

            long combats = 0;
            Integer maxCombats = null;

            long vagues = 0;
            Integer maxVagues = null;

            long ms = 0;
            Integer maxMs = null;

            boolean roundPresent = false;

            for (PlayerAnalyse player : analyseJoueurs) {
                if (player.rounds() == null) continue;
                for (RoundDetail r : player.rounds()) {
                    if (r.roundNum() == rNum) {
                        roundPresent = true;
                        pgDeployee += r.pgDeployee();
                        pointsCombat += r.pointsCombat();
                        if (r.maxPointsCombat() != null) {
                            maxPointsCombat = (maxPointsCombat == null ? 0L : maxPointsCombat) + r.maxPointsCombat();
                        }
                        combats += r.combats();
                        if (r.maxCombats() != null) {
                            maxCombats = (maxCombats == null ? 0 : maxCombats) + r.maxCombats();
                        }
                        vagues += r.vagues();
                        if (r.maxVagues() != null) {
                            maxVagues = (maxVagues == null ? 0 : maxVagues) + r.maxVagues();
                        }
                        ms += r.ms();
                        if (r.maxMs() != null) {
                            maxMs = (maxMs == null ? 0 : maxMs) + r.maxMs();
                        }
                    }
                }
            }

            if (roundPresent) {
                synthese.add(new RoundDetail(
                        rNum,
                        pgDeployee,
                        pointsCombat, maxPointsCombat, pourcentageL(pointsCombat, maxPointsCombat),
                        combats, maxCombats, pourcentageI(combats, maxCombats),
                        vagues, maxVagues, pourcentageI(vagues, maxVagues),
                        ms, maxMs, pourcentageI(ms, maxMs)
                ));
            }
        }

        return synthese;
    }

    private Map<Integer, RoundMax> calculerMaxParRound(Long planId) {
        List<TbPlanRound> rounds = tbPlanRoundRepository.findByPlanIdOrderByRoundNumAsc(planId);

        Map<Long, List<Integer>> roundsParPlanete = new HashMap<>();
        for (TbPlanRound r : rounds) {
            ajouterOccurrence(roundsParPlanete, r.getLsPlaneteId(), r.getRoundNum());
            ajouterOccurrence(roundsParPlanete, r.getDsPlaneteId(), r.getRoundNum());
            ajouterOccurrence(roundsParPlanete, r.getMixPlaneteId(), r.getRoundNum());
            ajouterOccurrence(roundsParPlanete, r.getZeffoPlaneteId(), r.getRoundNum());
            ajouterOccurrence(roundsParPlanete, r.getMandalorePlaneteId(), r.getRoundNum());
        }

        Map<Long, Integer> dernierRoundParPlanete = new HashMap<>();
        for (Map.Entry<Long, List<Integer>> e : roundsParPlanete.entrySet()) {
            dernierRoundParPlanete.put(e.getKey(), Collections.max(e.getValue()));
        }

        Map<Long, TbPlaneteReference> planetesParId = tbPlaneteReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(TbPlaneteReference::getId, p -> p));

        Map<Integer, RoundMax> resultat = new HashMap<>();
        for (TbPlanRound r : rounds) {
            long maxCombat = 0;
            int maxCombats = 0;
            int maxVagues = 0;
            int maxMs = 0;

            List<Long> planetesDuRound = Arrays.asList(
                    r.getLsPlaneteId(), r.getDsPlaneteId(), r.getMixPlaneteId(),
                    r.getZeffoPlaneteId(), r.getMandalorePlaneteId());

            for (Long planeteId : planetesDuRound) {
                if (planeteId == null) continue;
                TbPlaneteReference p = planetesParId.get(planeteId);
                if (p == null) continue;

                maxCombat += nz(p.getGpCombat());
                maxCombats += nzI(p.getToonStrikeClassic()) + nzI(p.getToonStrikeSpecial()) + nzI(p.getShipStrike());
                maxVagues += nzI(p.getVagues());

                if (Objects.equals(dernierRoundParPlanete.get(planeteId), r.getRoundNum())) {
                    maxMs += nzI(p.getMs());
                }
            }

            resultat.put(r.getRoundNum(), new RoundMax(maxCombat, maxCombats, maxVagues, maxMs));
        }

        return resultat;
    }

    private void ajouterOccurrence(Map<Long, List<Integer>> map, Long planeteId, Integer round) {
        if (planeteId != null) {
            map.computeIfAbsent(planeteId, k -> new ArrayList<>()).add(round);
        }
    }

    private Double pourcentageL(long valeur, Long max) {
        return (max == null || max == 0) ? null : 100.0 * valeur / max;
    }

    private Double pourcentageI(long valeur, Integer max) {
        return (max == null || max == 0) ? null : 100.0 * valeur / max;
    }

    private long nz(Long v) { return v != null ? v : 0L; }
    private int nzI(Integer v) { return v != null ? v : 0; }
}