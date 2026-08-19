package swgohManager.service;

import swgohManager.client.dto.GuildResponse;
import swgohManager.model.RaidHistorique;
import swgohManager.repository.RaidHistoriqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaidService {

    private final RaidHistoriqueRepository raidHistoriqueRepository;

    @Value("${swgoh.guild.id}")
    private String guildId;

    @Transactional
    public int enregistrerResultatsRaid(GuildResponse response) {
        List<GuildResponse.RecentRaidResult> raidResults = response.guild().recentRaidResult();

        if (raidResults == null || raidResults.isEmpty()) {
            log.warn("Aucun résultat de raid disponible pour la guilde {}", guildId);
            return 0;
        }

        GuildResponse.RecentRaidResult dernierRaid = raidResults.stream()
                .max(Comparator.comparingLong(GuildResponse.RecentRaidResult::endTime))
                .orElseThrow();

        Instant endTime = Instant.ofEpochSecond(dernierRaid.endTime());

        List<RaidHistorique> nouveauxResultats = dernierRaid.raidMember().stream()
                .filter(m -> !raidHistoriqueRepository.existsByGuildIdAndPlayerIdAndEndTime(guildId, m.playerId(), endTime))
                .map(m -> RaidHistorique.builder()
                        .guildId(guildId)
                        .playerId(m.playerId())
                        .score(m.memberProgress())
                        .endTime(endTime)
                        .build())
                .toList();

        raidHistoriqueRepository.saveAll(nouveauxResultats);
        log.info("Raid du {} : {} nouveaux résultats enregistrés (sur {} membres)",
                endTime, nouveauxResultats.size(), dernierRaid.raidMember().size());

        return nouveauxResultats.size();
    }
}