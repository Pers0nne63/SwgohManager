package swgohManager.service;

import swgohManager.client.dto.GuildResponse;
import swgohManager.model.TbActivite;
import swgohManager.model.TbScoreJoueur;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TbActiviteRepository;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.repository.TerritoryBattleRepository;
import swgohManager.util.MapStatIdParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryBattleService {

    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbActiviteRepository tbActiviteRepository;
    private final TbScoreJoueurRepository tbScoreJoueurRepository;

    @Value("${swgoh.guild.id}")
    private String guildId;

    @Transactional
    public String synchroniserTerritoryBattle(GuildResponse response) {
        List<GuildResponse.TerritoryBattleResult> tbResults = response.guild().recentTerritoryBattleResult();

        if (tbResults == null || tbResults.isEmpty()) {
            log.warn("Aucune Territory Battle disponible pour la guilde {}", guildId);
            return "Aucune Territory Battle trouvée";
        }

        // On prend la TB avec l'endTime le plus récent, au cas où plusieurs seraient renvoyées
        GuildResponse.TerritoryBattleResult tbData = tbResults.stream()
                .max(Comparator.comparingLong(r -> parseLong(r.endTime())))
                .orElseThrow();

        TerritoryBattle tb = territoryBattleRepository.findByInstanceId(tbData.instanceId())
                .orElseGet(TerritoryBattle::new);

        tb.setGuildId(guildId);
        tb.setInstanceId(tbData.instanceId());
        tb.setDefinitionId(tbData.definitionId());
        tb.setStartTime(Instant.ofEpochMilli(parseLong(tbData.startTime())));
        tb.setEndTime(Instant.ofEpochMilli(parseLong(tbData.endTime())));
        tb.setTotalStars(parseInt(tbData.totalStars()));
        tb = territoryBattleRepository.save(tb);

        // Charger l'existant en mémoire pour éviter le N+1 (une requête au lieu de ~190)
        Map<String, TbActivite> activitesExistantes = new HashMap<>();
        for (TbActivite a : tbActiviteRepository.findByTerritoryBattle(tb)) {
            activitesExistantes.put(a.getMapStatId(), a);
        }

        List<TbActivite> activitesAEnregistrer = new ArrayList<>();
        int nouvellesActivites = 0;

        for (GuildResponse.FinalStat fs : tbData.finalStat()) {
            if (!activitesExistantes.containsKey(fs.mapStatId())) {
                MapStatIdParser.ParsedMapStat parsed = MapStatIdParser.parse(fs.mapStatId());
                TbActivite activite = TbActivite.builder()
                        .territoryBattle(tb)
                        .mapStatId(fs.mapStatId())
                        .statType(parsed.statType())
                        .phase(parsed.phase())
                        .conflict(parsed.conflict())
                        .bonus(parsed.bonus())
                        .covertNum(parsed.covertNum())
                        .roundNum(parsed.roundNum())
                        .build();
                activitesAEnregistrer.add(activite);
                nouvellesActivites++;
            }
        }

        List<TbActivite> activitesSauvees = tbActiviteRepository.saveAll(activitesAEnregistrer);
        activitesSauvees.forEach(a -> activitesExistantes.put(a.getMapStatId(), a));

        // Traiter les scores : upsert par activité
        int scoresNouveaux = 0;
        int scoresMisAJour = 0;

        for (GuildResponse.FinalStat fs : tbData.finalStat()) {
            TbActivite activite = activitesExistantes.get(fs.mapStatId());

            Map<String, TbScoreJoueur> scoresExistants = new HashMap<>();
            for (TbScoreJoueur s : tbScoreJoueurRepository.findByTbActivite(activite)) {
                scoresExistants.put(s.getPlayerId(), s);
            }

            List<TbScoreJoueur> aSauvegarder = new ArrayList<>();

            for (GuildResponse.PlayerStat ps : fs.playerStat()) {
                Long score = parseLongOrNull(ps.score());
                TbScoreJoueur existant = scoresExistants.get(ps.memberId());

                if (existant != null) {
                    if (!Objects.equals(existant.getScore(), score)) {
                        existant.setScore(score);
                        aSauvegarder.add(existant);
                        scoresMisAJour++;
                    }
                } else {
                    aSauvegarder.add(TbScoreJoueur.builder()
                            .tbActivite(activite)
                            .playerId(ps.memberId())
                            .score(score)
                            .build());
                    scoresNouveaux++;
                }
            }

            tbScoreJoueurRepository.saveAll(aSauvegarder);
        }

        String resultat = String.format(
                "TB %s : %d nouvelle(s) activité(s), %d nouveau(x) score(s), %d score(s) mis à jour",
                tb.getInstanceId(), nouvellesActivites, scoresNouveaux, scoresMisAJour);
        log.info(resultat);
        return resultat;
    }

    private long parseLong(String value) {
        return Long.parseLong(value);
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}