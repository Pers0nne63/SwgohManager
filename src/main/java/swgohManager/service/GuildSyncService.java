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
    private final GuildeService guildeService;
    private final TerritoryBattleService territoryBattleService;
    private final TerritoryWarService territoryWarService;
    private final TbPlaneteReferenceService tbPlaneteReferenceService;

    @Value("${swgoh.guild.id}")
    private String guildId;

    public GuildSyncResult synchroniserGuilde() {
        String resultatRefPlanetes = tbPlaneteReferenceService.seedDonnees();
        log.info("Référentiel TB : {}", resultatRefPlanetes);

        log.info("Appel unique à l'API SWGOH pour la guilde {}", guildId);
        GuildResponse response = swgohApiClient.getGuild(guildId);

        int nouveauxRaids = raidService.enregistrerResultatsRaid(response);
        String resultatGuilde = guildeService.synchroniserGuilde(response);
        String resultatTb = territoryBattleService.synchroniserTerritoryBattle(response);
        String resultatTw = territoryWarService.synchroniserTerritoryWar(response);

        return new GuildSyncResult(nouveauxRaids, resultatGuilde, resultatTb, resultatTw, resultatRefPlanetes);
    }

    public record GuildSyncResult(
            int nouveauxResultatsRaid,
            String resultatJoueurs,
            String resultatTerritoryBattle,
            String resultatTerritoryWar, 
            String resultatRefPlanetes
    ) {}
}