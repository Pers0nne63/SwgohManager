package swgohManager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.GuildResponse;
import swgohManager.model.ExternalPlayerRaid;
import swgohManager.model.ExternalPlayerTbScore;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.util.MapStatIdParser;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRaidTbSyncService {

    private final SwgohApiClient swgohApiClient;
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;

    @Transactional
    public void synchroniser(String guildId, String playerId) {
        if (guildId == null || guildId.isBlank()) {
            log.warn("Pas de guildId pour le joueur externe {}, raid/TB non synchronisés", playerId);
            return;
        }

        GuildResponse response = swgohApiClient.getGuild(guildId);
        if (response == null || response.guild() == null) {
            log.warn("Réponse /guild vide pour guildId={}", guildId);
            return;
        }

        synchroniserRaid(response, guildId, playerId);
        synchroniserTb(response, guildId, playerId);
    }

    private void synchroniserRaid(GuildResponse response, String guildId, String playerId) {
        List<GuildResponse.RecentRaidResult> raidResults = response.guild().recentRaidResult();
        if (raidResults == null || raidResults.isEmpty()) return;

        GuildResponse.RecentRaidResult dernierRaid = raidResults.stream()
                .max(Comparator.comparingLong(GuildResponse.RecentRaidResult::endTime))
                .orElse(null);
        if (dernierRaid == null) return;

        dernierRaid.raidMember().stream()
                .filter(m -> playerId.equals(m.playerId()))
                .findFirst()
                .ifPresent(m -> {
                    externalPlayerRaidRepository.deleteByPlayerId(playerId);
                    externalPlayerRaidRepository.flush();
                    externalPlayerRaidRepository.save(ExternalPlayerRaid.builder()
                            .playerId(playerId)
                            .guildId(guildId)
                            .score(m.memberProgress())
                            .endTime(Instant.ofEpochSecond(dernierRaid.endTime()))
                            .build());
                });
    }

    private void synchroniserTb(GuildResponse response, String guildId, String playerId) {
        List<GuildResponse.TerritoryBattleResult> tbResults = response.guild().recentTerritoryBattleResult();
        if (tbResults == null || tbResults.isEmpty()) return;

        GuildResponse.TerritoryBattleResult tbData = tbResults.stream()
                .max(Comparator.comparingLong(r -> Long.parseLong(r.endTime())))
                .orElse(null);
        if (tbData == null) return;

        Instant startTime = Instant.ofEpochMilli(Long.parseLong(tbData.startTime()));
        Instant endTime = Instant.ofEpochMilli(Long.parseLong(tbData.endTime()));
        Integer totalStars = parseIntOrNull(tbData.totalStars());

        List<ExternalPlayerTbScore> lignes = new ArrayList<>();
        for (GuildResponse.FinalStat fs : tbData.finalStat()) {
            fs.playerStat().stream()
                    .filter(ps -> playerId.equals(ps.memberId()))
                    .findFirst()
                    .ifPresent(ps -> {
                        MapStatIdParser.ParsedMapStat parsed = MapStatIdParser.parse(fs.mapStatId());
                        lignes.add(ExternalPlayerTbScore.builder()
                                .playerId(playerId)
                                .guildId(guildId)
                                .tbInstanceId(tbData.instanceId())
                                .tbDefinitionId(tbData.definitionId())
                                .tbStartTime(startTime)
                                .tbEndTime(endTime)
                                .totalStars(totalStars)
                                .mapStatId(fs.mapStatId())
                                .statType(parsed.statType())
                                .phase(parsed.phase())
                                .conflict(parsed.conflict())
                                .bonus(parsed.bonus())
                                .covertNum(parsed.covertNum())
                                .roundNum(parsed.roundNum())
                                .score(parseLongOrNull(ps.score()))
                                .build());
                    });
        }

        if (!lignes.isEmpty()) {
            externalPlayerTbScoreRepository.deleteByPlayerId(playerId);
            externalPlayerTbScoreRepository.flush();
            externalPlayerTbScoreRepository.saveAll(lignes);
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return null; }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }
}