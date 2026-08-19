package swgohManager.service;

import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.LeaderboardMod;
import swgohManager.model.LeaderboardUnit;
import swgohManager.repository.LeaderboardModRepository;
import swgohManager.repository.LeaderboardUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardRosterService {

    private final LeaderboardUnitRepository leaderboardUnitRepository;
    private final LeaderboardModRepository leaderboardModRepository;

    @Transactional
    public void enregistrerRoster(PlayerResponse response) {
        String playerId = response.playerId();
        List<PlayerResponse.RosterUnit> roster = response.rosterUnit();

        if (roster == null || roster.isEmpty()) {
            log.warn("Aucune unité dans le roster GAC pour le joueur {}", playerId);
            return;
        }

        // Pas d'historisation : on remplace intégralement l'ancien état
        leaderboardUnitRepository.deleteByPlayerId(playerId);
        leaderboardModRepository.deleteByPlayerId(playerId);
        
     // FORCER Hibernate à envoyer les DELETE immédiatement à la BDD
        leaderboardUnitRepository.flush();
        leaderboardModRepository.flush();

        List<LeaderboardUnit> unites = new ArrayList<>();
        List<LeaderboardMod> mods = new ArrayList<>();

        for (PlayerResponse.RosterUnit u : roster) {
            Integer relicValue = u.relic() != null && u.relic().currentTier() != null
                    ? u.relic().currentTier() - 2 : null;

            unites.add(LeaderboardUnit.builder()
                    .playerId(playerId).idUnit(u.id()).definitionId(u.definitionId())
                    .etoiles(u.currentRarity()).niveau(u.currentLevel()).gear(u.currentTier()).relic(relicValue)
                    .build());

            if (u.equippedStatMod() != null) {
                for (PlayerResponse.EquippedStatMod mod : u.equippedStatMod()) {
                    mods.addAll(construireLignesMod(playerId, u.id(), mod));
                }
            }
        }

        leaderboardUnitRepository.saveAll(unites);
        leaderboardModRepository.saveAll(mods);

        log.info("Roster GAC enregistré pour {} : {} unité(s), {} ligne(s) de mod", playerId, unites.size(), mods.size());
    }

    private List<LeaderboardMod> construireLignesMod(String playerId, String idUnit, PlayerResponse.EquippedStatMod mod) {
        List<LeaderboardMod> lignes = new ArrayList<>();

        String definitionId = mod.definitionId();
        String set = definitionId != null && definitionId.length() >= 1 ? definitionId.substring(0, 1) : null;
        String rarity = definitionId != null && definitionId.length() >= 2 ? definitionId.substring(1, 2) : null;
        String position = definitionId != null && definitionId.length() >= 3 ? definitionId.substring(2, 3) : null;

        Integer idPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? mod.primaryStat().stat().unitStatId() : null;
        Long valeurPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? parseLong(mod.primaryStat().stat().unscaledDecimalValue()) : null;

        List<PlayerResponse.SecondaryStat> secondaires = mod.secondaryStat() != null ? mod.secondaryStat() : List.of();

        int ordre = 1;
        for (PlayerResponse.SecondaryStat secondaryStat : secondaires) {
            Integer idSecondaire = secondaryStat.stat() != null ? secondaryStat.stat().unitStatId() : null;
            Long valeurSecondaire = secondaryStat.stat() != null
                    ? parseLong(secondaryStat.stat().unscaledDecimalValue()) : null;

            lignes.add(LeaderboardMod.builder()
                    .playerId(playerId).idUnit(idUnit).idMod(mod.id()).definitionId(definitionId)
                    .set(set).rarity(rarity).position(position).niveau(mod.level())
                    .idPrimaire(idPrimaire).valeurPrimaire(valeurPrimaire)
                    .idSecondaire(idSecondaire).valeurSecondaire(valeurSecondaire)
                    .ordreSecondaire(ordre)
                    .build());
            ordre++;
        }

        return lignes;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique du mod : {}", value);
            return null;
        }
    }
}