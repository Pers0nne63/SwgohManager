package swgohManager.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import swgohManager.model.GuildBilanActuel;
import swgohManager.model.Joueur;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.GuildBilanActuelRepository;
import swgohManager.repository.JoueurRepository;
import swgohManager.repository.PlayerDatacronActuelRepository;
import swgohManager.repository.PlayerDatacronAffixActuelRepository;
import swgohManager.repository.PlayerModQActuelRepository;
import swgohManager.repository.PlayerStatqActuelRepository;
import swgohManager.repository.PlayerStatqDetailActuelRepository;
import swgohManager.repository.RaidHistoriqueRepository;
import swgohManager.repository.TerritoryBattleRepository;
import swgohManager.repository.TwHistoriqueRepository;

@Service
@RequiredArgsConstructor
public class GuildBilanService {

    private static final String MODE_TB = "TERRITORY_BATTLE_BOTH_OMICRON";
    private static final String MODE_TW = "TERRITORY_WAR_OMICRON";

    private final JoueurRepository joueurRepository;
    private final GuildOverviewService guildOverviewService;
    private final PlayerModQActuelRepository playerModQActuelRepository;
    private final PlayerStatqActuelRepository playerStatqActuelRepository;
    private final PlayerStatqDetailActuelRepository playerStatqDetailActuelRepository;
    private final PlayerDatacronActuelRepository playerDatacronActuelRepository;
    private final PlayerDatacronAffixActuelRepository playerDatacronAffixActuelRepository;
    private final OmicronModeService omicronModeService;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final RaidHistoriqueRepository raidHistoriqueRepository;
    private final GuildBilanActuelRepository guildBilanActuelRepository;
    private final ObjectMapper objectMapper;
    private final TwHistoriqueRepository twHistoriqueRepository;


    @Transactional
    public GuildBilanActuel rafraichir() {
        List<Joueur> joueurs = joueurRepository.findAllByPresentInGuildTrue();
        String guildId = joueurs.isEmpty() ? null : joueurs.get(0).getGuildId();
        var modq = playerModQActuelRepository.findAggregatGuilde();
        var relics = guildOverviewService.getRepartitionRelics();
        var statqTeams = playerStatqDetailActuelRepository.findMoyenneNoteParTeam();
        var twStats = guildId != null
                ? twHistoriqueRepository.findStatsAgregeesByGuildId(guildId)
                : null;

        Map<String, Long> omicronParMode = omicronModeService.getSyntheseGuilde();
        long omicronTb = omicronParMode.getOrDefault(MODE_TB, 0L);
        long omicronTw = omicronParMode.getOrDefault(MODE_TW, 0L);

        Integer etoilesBT = territoryBattleRepository.findTopByOrderByEndTimeDesc()
                .map(TerritoryBattle::getTotalStars).orElse(null);

        Long scoreRaidTotal = raidHistoriqueRepository.findTopByOrderByEndTimeDesc()
                .map(dernier -> raidHistoriqueRepository.findByEndTime(dernier.getEndTime()).stream()
                        .mapToLong(r -> r.getScore() != null ? r.getScore() : 0L).sum())
                .orElse(null);
        
       

        GuildBilanActuel bilan = GuildBilanActuel.builder()
                .dateCalcul(Instant.now())
                .guildId(guildId)
                .guildNom(joueurs.isEmpty() ? null : joueurs.get(0).getGuildName())
                .nbMembres(joueurs.size())
                .galacticPowerTotal(joueurRepository.sumGalacticPowerGuilde())
                .etoilesBT(etoilesBT)
                .scoreRaidTotal(scoreRaidTotal)
                .modQMoyen(modq.getModQMoyen())
                .mod25Plus(modq.getMod25Plus())
                .mod20A24(modq.getMod20A24())
                .mod15A19(modq.getMod15A19())
                .statQMoyen(playerStatqActuelRepository.findMoyenneStatqGuilde())
                .relic10(relics.getRelic10())
                .relic9(relics.getRelic9())
                .relic8(relics.getRelic8())
                .relic6Et7(relics.getRelic6Et7())
                .relic0A5(relics.getRelic0A5())
                .sansRelic(relics.getSansRelic())
                .nbFdtc(playerDatacronActuelRepository.countFdtcGuilde())
                .nbDtc9(playerDatacronAffixActuelRepository.countDtc9Guilde())
                .nbOmicronTb(omicronTb)
                .nbOmicronTw(omicronTw)
                .statQParTeamJson(serialiser(statqTeams))
                .twNombreAnalysees(twStats != null ? twStats.getNombre() : 0L)
                .twPgInscriteMoyenne(twStats != null ? twStats.getPgInscriteMoyenne() : null)
                .twScoreMoyen(twStats != null ? twStats.getScoreMoyen() : null)
                .twScoreAdversaireMoyen(twStats != null ? twStats.getScoreAdversaireMoyen() : null)
                .twVictoires(twStats != null ? twStats.getVictoires() : 0L)
                .twDefaites(twStats != null ? twStats.getDefaites() : 0L)
                .twEcartMoyen(twStats != null ? twStats.getEcartMoyen() : null)
                .build();

        guildBilanActuelRepository.deleteAll();
        guildBilanActuelRepository.flush();
        return guildBilanActuelRepository.save(bilan);
    }

    public GuildBilanActuel getBilanActuelOuRafraichir() {
        return guildBilanActuelRepository.findAll().stream().findFirst()
                .orElseGet(this::rafraichir);
    }

    private String serialiser(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }
}