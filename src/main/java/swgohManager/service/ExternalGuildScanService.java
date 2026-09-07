package swgohManager.service;

import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.GuildResponse;
import swgohManager.model.ExternalPlayerRaid;
import swgohManager.model.ExternalPlayerTbScore;
import swgohManager.repository.ExternalPlayerRaidRepository;
import swgohManager.repository.ExternalPlayerTbScoreRepository;
import swgohManager.util.MapStatIdParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalGuildScanService {

    private final SwgohApiClient swgohApiClient;
    private final ExternalPlayerRaidRepository externalPlayerRaidRepository;
    private final ExternalPlayerTbScoreRepository externalPlayerTbScoreRepository;
    public record GuildPlayerData(
            String guildName,
            Long galacticPower,
            Long characterGalacticPower,
            Long shipGalacticPower
    ) {}

    @Transactional
    public GuildPlayerData scannerGuildeDuJoueur(String playerId, String guildId) {
    	if (guildId == null || guildId.isBlank()) {
            log.warn("Pas de guildId disponible pour le joueur {}, scan guilde ignoré", playerId);
            return null;
        }

        GuildResponse response = swgohApiClient.getGuild(guildId);
        if (response == null || response.guild() == null) {
            log.warn("Réponse de guilde invalide pour guildId={}", guildId);
            return null;
        }

        externalPlayerRaidRepository.deleteByPlayerId(playerId);
        externalPlayerTbScoreRepository.deleteByPlayerId(playerId);
        externalPlayerRaidRepository.flush();
        externalPlayerTbScoreRepository.flush();

        enregistrerRaid(response, playerId, guildId);
        enregistrerTb(response, playerId, guildId);

        // 👇 Recherche du membre spécifique dans la guilde pour extraire ses GP
        return extraireInfosMembre(response, playerId);
    }
    
    private GuildPlayerData extraireInfosMembre(GuildResponse response, String playerId) {
        // 👈 Utilisation de getGuildName() au lieu de name()
        String guildName = response.guild().getGuildName(); 
        List<GuildResponse.Member> members = response.guild().member();

        if (members == null || members.isEmpty()) {
            return new GuildPlayerData(guildName, null, null, null);
        }

        return members.stream()
                .filter(m -> playerId.equals(m.playerId()))
                .findFirst()
                .map(m -> new GuildPlayerData(
                        guildName,
                        parseLongOrNull(m.galacticPower()),
                        parseLongOrNull(m.characterGalacticPower()),
                        parseLongOrNull(m.shipGalacticPower())
                ))
                .orElse(new GuildPlayerData(guildName, null, null, null));
    }

    private void enregistrerRaid(GuildResponse response, String playerId, String guildId) {
        List<GuildResponse.RecentRaidResult> raids = response.guild().recentRaidResult();
        if (raids == null || raids.isEmpty()) return;

        GuildResponse.RecentRaidResult dernierRaid = raids.stream()
                .max(Comparator.comparingLong(GuildResponse.RecentRaidResult::endTime))
                .orElse(null);
        if (dernierRaid == null || dernierRaid.raidMember() == null) return;

        dernierRaid.raidMember().stream()
                .filter(m -> playerId.equals(m.playerId()))
                .findFirst()
                .ifPresentOrElse(
                        m -> externalPlayerRaidRepository.save(ExternalPlayerRaid.builder()
                                .playerId(playerId).guildId(guildId)
                                .score(m.memberProgress())
                                .endTime(Instant.ofEpochSecond(dernierRaid.endTime()))
                                .build()),
                        () -> log.warn("Joueur {} absent du dernier résultat de raid de sa guilde", playerId)
                );
    }

    private void enregistrerTb(GuildResponse response, String playerId, String guildId) {
        List<GuildResponse.TerritoryBattleResult> tbResults = response.guild().recentTerritoryBattleResult();
        if (tbResults == null || tbResults.isEmpty()) return;

        GuildResponse.TerritoryBattleResult tbData = tbResults.stream()
                .max(Comparator.comparingLong(r -> parseLong(r.endTime())))
                .orElse(null);
        if (tbData == null || tbData.finalStat() == null) return;

        Instant startTime = Instant.ofEpochMilli(parseLong(tbData.startTime()));
        Instant endTime = Instant.ofEpochMilli(parseLong(tbData.endTime()));
        Integer totalStars = parseInt(tbData.totalStars());

        List<ExternalPlayerTbScore> lignes = new ArrayList<>();

        for (GuildResponse.FinalStat fs : tbData.finalStat()) {
            if (fs.playerStat() == null) continue;
            for (GuildResponse.PlayerStat ps : fs.playerStat()) {
                if (!playerId.equals(ps.memberId())) continue;

                MapStatIdParser.ParsedMapStat parsed = MapStatIdParser.parse(fs.mapStatId());

                lignes.add(ExternalPlayerTbScore.builder()
                        .playerId(playerId).guildId(guildId)
                        .tbInstanceId(tbData.instanceId()).tbDefinitionId(tbData.definitionId())
                        .tbStartTime(startTime).tbEndTime(endTime).totalStars(totalStars)
                        .mapStatId(fs.mapStatId()).statType(parsed.statType())
                        .phase(parsed.phase()).conflict(parsed.conflict())
                        .bonus(parsed.bonus()).covertNum(parsed.covertNum()).roundNum(parsed.roundNum())
                        .score(parseLongOrNull(ps.score()))
                        .build());
            }
        }

        externalPlayerTbScoreRepository.saveAll(lignes);
        log.info("TB de la guilde de {} : {} ligne(s) de score enregistrée(s)", playerId, lignes.size());
    }

    private long parseLong(String value) { return Long.parseLong(value); }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }
}