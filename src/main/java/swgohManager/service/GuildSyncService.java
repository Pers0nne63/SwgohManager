package swgohManager.service;

import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.GuildResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildSyncService {

    private final SwgohApiClient swgohApiClient;
    private final RaidService raidService;
    private final JoueurService joueurService;
    private final TerritoryBattleService territoryBattleService;

    @Value("${swgoh.guild.id}")
    private String guildId;

    public GuildSyncResult synchroniserGuilde() {
        log.info("Appel unique à l'API SWGOH pour la guilde {}", guildId);
        GuildResponse response = swgohApiClient.getGuild(guildId);

        int nouveauxRaids = raidService.enregistrerResultatsRaid(response);
        String resultatJoueurs = joueurService.synchroniserJoueurs(response);
        String resultatTb = territoryBattleService.synchroniserTerritoryBattle(response);

        return new GuildSyncResult(nouveauxRaids, resultatJoueurs, resultatTb);
    }

    public record GuildSyncResult(int nouveauxResultatsRaid, String resultatJoueurs, String resultatTerritoryBattle) {}
}