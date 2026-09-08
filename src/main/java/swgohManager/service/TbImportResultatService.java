package swgohManager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.dto.TbImportResultatsDto.JoueurImportDTO;
import swgohManager.dto.TbImportResultatsDto.PlanetStatDTO;
import swgohManager.dto.TbImportResultatsDto.RoundImportDTO;
import swgohManager.dto.TbScoreFlatProjection;
import swgohManager.repository.TbImportScoreJoueurRepository;

@Service
@RequiredArgsConstructor
public class TbImportResultatService {

    private final TbImportScoreJoueurRepository tbImportScoreJoueurRepository;

    public List<RoundImportDTO> getResultatsImportForTb(Long tbId) {
        
        List<TbScoreFlatProjection> rows = tbImportScoreJoueurRepository.findScoresByTbIdNative(tbId);
        if (rows.isEmpty()) return Collections.emptyList();

        // Structure : Map<RoundNum, Map<PlayerName, Map<Zone, PlanetStatDTO>>>
        Map<Integer, Map<String, Map<String, PlanetStatDTO>>> aggr = new TreeMap<>();
        
        // Noms des planètes par round : Map<RoundNum, Map<Zone, NomPlanete>>
        Map<Integer, Map<String, String>> roundPlanetNames = new TreeMap<>();

     // 1. Première passe : Accumulation des données brutes
        for (TbScoreFlatProjection row : rows) {
            Integer round = row.getRoundNum();
            if (round == null) continue;

            String playerName = row.getPlayerName() != null ? row.getPlayerName() : "Joueur Inconnu";
            String zone = row.getZone() != null ? row.getZone().trim() : "Autre";
            String planeteName = row.getPlaneteName();

            if (planeteName != null && !planeteName.isBlank() && !"Autre".equals(zone)) {
                roundPlanetNames.putIfAbsent(round, new HashMap<>());
                roundPlanetNames.get(round).put(zone, planeteName);
            }

            if (!Set.of("LS", "DS", "Mix", "Zeffo", "Mandalore").contains(zone)) {
                continue;
            }

            String type = row.getStatType();
            Long val = row.getScore() != null ? row.getScore() : 0L;

            aggr.putIfAbsent(round, new HashMap<>());
            aggr.get(round).putIfAbsent(playerName, new HashMap<>());
            
            aggr.get(round).get(playerName).putIfAbsent(zone, PlanetStatDTO.builder()
                    .combats(0)
                    .vagues(0)
                    .pgDeploye(0L)
                    .pointsCombat(0L)
                    .rawSummary(0L)
                    .build());

            PlanetStatDTO stats = aggr.get(round).get(playerName).get(zone);

            if (type != null) {
                String lowerType = type.toLowerCase();
                if (lowerType.contains("strike_attempt")) {
                    stats.setCombats((stats.getCombats() != null ? stats.getCombats() : 0) + val.intValue());
                } else if (lowerType.contains("strike_encounter")) {
                    stats.setVagues((stats.getVagues() != null ? stats.getVagues() : 0) + val.intValue());
                } else if (lowerType.contains("power")) {
                    stats.setPgDeploye((stats.getPgDeploye() != null ? stats.getPgDeploye() : 0) + val);
                } else if (lowerType.contains("summary")) {
                    stats.setRawSummary((stats.getRawSummary() != null ? stats.getRawSummary() : 0L) + val);
                }
            }
        }

        // 2. Seconde passe : Calcul des points de combat (summary(R) - power(R) - summary(R-1))
        for (Integer roundNum : aggr.keySet()) {
            Map<String, Map<String, PlanetStatDTO>> joueursDuRound = aggr.get(roundNum);
            
            for (Map.Entry<String, Map<String, PlanetStatDTO>> entryJoueur : joueursDuRound.entrySet()) {
                String player = entryJoueur.getKey();
                
                for (Map.Entry<String, PlanetStatDTO> entryZone : entryJoueur.getValue().entrySet()) {
                    String zone = entryZone.getKey();
                    PlanetStatDTO stats = entryZone.getValue();

                    Long summaryActuel = stats.getRawSummary() != null ? stats.getRawSummary() : 0L;
                    Long powerActuel = stats.getPgDeploye() != null ? stats.getPgDeploye() : 0L;
                    
                    // Recherche du summary au round précédent (R-1)
                    Long summaryPrecedent = 0L;
                    if (aggr.containsKey(roundNum - 1) 
                            && aggr.get(roundNum - 1).containsKey(player) 
                            && aggr.get(roundNum - 1).get(player).containsKey(zone)) {
                        
                        PlanetStatDTO statsPrecedentes = aggr.get(roundNum - 1).get(player).get(zone);
                        if (statsPrecedentes.getRawSummary() != null) {
                            summaryPrecedent = statsPrecedentes.getRawSummary();
                        }
                    }

                    // Calcul final
                    Long ptsCombat = summaryActuel - powerActuel - summaryPrecedent;
                    stats.setPointsCombat(ptsCombat);
                }
            }
        }

        List<RoundImportDTO> result = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, Map<String, PlanetStatDTO>>> entryRound : aggr.entrySet()) {
            Integer roundNum = entryRound.getKey();
            List<JoueurImportDTO> joueurs = new ArrayList<>();

            for (Map.Entry<String, Map<String, PlanetStatDTO>> entryJoueur : entryRound.getValue().entrySet()) {
                joueurs.add(JoueurImportDTO.builder()
                        .playerName(entryJoueur.getKey())
                        .planetStats(entryJoueur.getValue())
                        .build());
            }

            joueurs.sort(Comparator.comparing(JoueurImportDTO::getPlayerName, String.CASE_INSENSITIVE_ORDER));

            result.add(RoundImportDTO.builder()
                    .roundNum(roundNum)
                    .planetNames(roundPlanetNames.getOrDefault(roundNum, Collections.emptyMap()))
                    .joueurs(joueurs)
                    .build());
        }

        return result;
    }
    
}