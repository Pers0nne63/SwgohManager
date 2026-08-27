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
    
    // Ajout de l'injection du service de référence
    private final TbPlaneteReferenceService tbPlaneteReferenceService;

    @Value("${swgoh.guild.id}")
    private String guildId;

    public GuildSyncResult synchroniserGuilde() {
        // 1. Mise à jour / Vérification du référentiel des planètes TB avant le reste
        String resultatRefPlanetes = tbPlaneteReferenceService.seedDonnees();
        log.info("Référentiel TB : {}", resultatRefPlanetes);

        // 2. Appel API pour la guilde
        log.info("Appel unique à l'API SWGOH pour la guilde {}", guildId);
        GuildResponse response = swgohApiClient.getGuild(guildId);

        // 3. Traitements de synchronisation
        int nouveauxRaids = raidService.enregistrerResultatsRaid(response);
        String resultatJoueurs = joueurService.synchroniserJoueurs(response);
        String resultatTb = territoryBattleService.synchroniserTerritoryBattle(response);

        // 4. Retour enrichi
        return new GuildSyncResult(nouveauxRaids, resultatJoueurs, resultatTb, resultatRefPlanetes);
    }

    // Mise à jour du record pour inclure le message du référentiel
    public record GuildSyncResult(
            int nouveauxResultatsRaid, 
            String resultatJoueurs, 
            String resultatTerritoryBattle,
            String resultatRefPlanetes
    ) {}
}