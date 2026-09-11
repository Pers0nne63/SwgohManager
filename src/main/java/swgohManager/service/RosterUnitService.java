package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.UnitModDTO;
import swgohManager.model.ExternalRosterUnitSkillActuel;
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.RosterUnitHistoriqueRepository;
import swgohManager.repository.RosterUnitModActuelRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import swgohManager.repository.SkillDefinitionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitService {

    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final RosterUnitHistoriqueRepository rosterUnitHistoriqueRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;
    private final ExternalRosterUnitSkillActuelRepository externalRosterUnitSkillActuelRepository;
    private final RosterUnitModActuelRepository rosterUnitModActuelRepository;
    private final SkillDefinitionRepository skillDefinitionRepository;
    private final PlayerModQService playerModQService;
    private final RosterUnitStatCalculService rosterUnitStatCalculService;
    private final FarmPlanProgressService farmPlanProgressService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final OmicronModeService omicronModeService;
    private final UnitSkillCalculationService unitSkillCalculationService;
    private final UnitModCalculationService unitModCalculationService;
    private final GuildSyncReferentialService guildSyncReferentialService;

    public Map<String, SkillDefinition> chargerDefinitionsSkill() {
        return skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));
    }

    /**
     * Calcule et persiste les skills (zeta/omicron) d'un joueur, interne ou externe.
     * Portee.GUILDE : table avec idSync. Portee.EXTERNE : table "actuel" sans idSync.
     * @return le résultat du calcul (DTO pivot), utilisable pour le comptage et l'appel à OmicronModeService.
     */
    @Transactional
    public UnitSkillCalculationService.UnitSkillBuildResult enregistrerSkills(String playerId,
            List<PlayerResponse.RosterUnit> roster, Map<String, SkillDefinition> definitions,
            Portee portee, Long idSync) {

        UnitSkillCalculationService.UnitSkillBuildResult buildResult =
                unitSkillCalculationService.construireSkillsDto(roster, definitions);

        if (portee == Portee.GUILDE) {
            rosterUnitSkillActuelRepository.deleteByPlayerId(playerId);
            rosterUnitSkillActuelRepository.flush();

            List<RosterUnitSkillActuel> entites = buildResult.skills().stream()
                    .map(dto -> RosterUnitSkillActuel.builder()
                            .playerId(playerId).idUnit(dto.idUnit()).idSkill(dto.idSkill())
                            .tier(dto.tier()).type(dto.type()).numero(dto.numero())
                            .skillZeta(dto.skillZeta()).zetaApplied(dto.zetaApplied())
                            .skillOmicron(dto.skillOmicron()).omicronApplied(dto.omicronApplied())
                            .idSync(idSync)
                            .build())
                    .toList();
            rosterUnitSkillActuelRepository.saveAll(entites);
        } else {
            externalRosterUnitSkillActuelRepository.deleteByPlayerId(playerId);
            externalRosterUnitSkillActuelRepository.flush();

            List<ExternalRosterUnitSkillActuel> entites = buildResult.skills().stream()
                    .map(dto -> ExternalRosterUnitSkillActuel.builder()
                            .playerId(playerId).idUnit(dto.idUnit()).idSkill(dto.idSkill())
                            .tier(dto.tier()).type(dto.type()).numero(dto.numero())
                            .skillZeta(dto.skillZeta()).zetaApplied(dto.zetaApplied())
                            .skillOmicron(dto.skillOmicron()).omicronApplied(dto.omicronApplied())
                            .build())
                    .toList();
            externalRosterUnitSkillActuelRepository.saveAll(entites);
        }

        return buildResult;
    }

    @Transactional
    public String enregistrerRoster(PlayerResponse response, Long idSync) {
        return enregistrerRoster(response, idSync, guildSyncReferentialService.charger());
    }

    /** Variante rapide pour la synchro de masse : référentiel commun déjà chargé une fois pour tout le lot. */
    @Transactional
    public String enregistrerRoster(PlayerResponse response, Long idSync, GuildSyncReferentialCache cache) {
        String playerId = response.playerId();
        List<PlayerResponse.RosterUnit> roster = response.rosterUnit();

        if (roster == null || roster.isEmpty()) {
            log.warn("Aucune unité dans le roster pour le joueur {}", playerId);
            return "Aucune unité trouvée";
        }

        StopWatch stopWatch = new StopWatch("Enregistrement Roster - Joueur " + playerId);

        stopWatch.start("Récupération référentiel (cache)");
        Map<String, SkillDefinition> definitions = cache.skillDefinitions();
        stopWatch.stop();

        stopWatch.start("Suppression DB (anciennes données)");
        rosterUnitActuelRepository.deleteByPlayerId(playerId);
        rosterUnitModActuelRepository.deleteByPlayerId(playerId);
        rosterUnitActuelRepository.flush();
        rosterUnitModActuelRepository.flush();
        stopWatch.stop();

        stopWatch.start("Mapping objets en mémoire");
        List<RosterUnitActuel> unitesActuelles = new ArrayList<>();
        List<RosterUnitModActuel> modsActuels = new ArrayList<>();

        for (PlayerResponse.RosterUnit u : roster) {
            Integer relicValue = u.relic() != null && u.relic().currentTier() != null
                    ? u.relic().currentTier() - 2 : null;

            unitesActuelles.add(RosterUnitActuel.builder()
                    .playerId(playerId).idUnit(u.id()).definitionId(u.definitionId())
                    .etoiles(u.currentRarity()).niveau(u.currentLevel()).gear(u.currentTier()).relic(relicValue)
                    .idSync(idSync)
                    .build());

            if (u.equippedStatMod() != null) {
                for (PlayerResponse.EquippedStatMod mod : u.equippedStatMod()) {
                    List<UnitModDTO> lignesDto = unitModCalculationService.construireLignesModDto(u.id(), mod);
                    for (UnitModDTO dto : lignesDto) {
                        modsActuels.add(RosterUnitModActuel.builder()
                                .playerId(playerId)
                                .idUnit(dto.idUnit())
                                .idMod(dto.idMod())
                                .definitionId(dto.definitionId())
                                .set(dto.set())
                                .rarity(dto.rarity())
                                .position(dto.position())
                                .niveau(dto.niveau())
                                .idPrimaire(dto.idPrimaire())
                                .valeurPrimaire(dto.valeurPrimaire())
                                .idSecondaire(dto.idSecondaire())
                                .valeurSecondaire(dto.valeurSecondaire())
                                .ordreSecondaire(dto.ordreSecondaire())
                                .idSync(idSync)
                                .build());
                    }
                }
            }
        }
        stopWatch.stop();

        stopWatch.start("Enregistrement Skills");
        UnitSkillCalculationService.UnitSkillBuildResult buildResult =
                enregistrerSkills(playerId, roster, definitions, Portee.GUILDE, idSync);
        int skillsSansDefinition = buildResult.skillsSansDefinition();
        stopWatch.stop();

        stopWatch.start("SaveAll DB (Unités & Mods)");
        rosterUnitActuelRepository.saveAll(unitesActuelles);
        rosterUnitModActuelRepository.saveAll(modsActuels);
        stopWatch.stop();

        if (skillsSansDefinition > 0) {
            log.warn("{} skill(s) sans correspondance dans skill_definition (référentiel pas encore synchronisé ?)",
                    skillsSansDefinition);
        }

        String resultat = String.format(
                "Sync #%d : %d unité(s), %d skill(s) (%d sans référentiel), %d ligne(s) de mod",
                idSync, unitesActuelles.size(), buildResult.skills().size(), skillsSansDefinition, modsActuels.size());

        stopWatch.start("playerModQService");
        playerModQService.calculerEtEnregistrer(playerId, modsActuels, idSync);
        stopWatch.stop();

        stopWatch.start("farmPlanProgressService");
        farmPlanProgressService.calculerEtEnregistrer(playerId, idSync, cache.farmPlans(), cache.unitLibelleByBaseId());
        stopWatch.stop();

        stopWatch.start("Services Omicron");
        omicronPlanProgressService.calculerEtEnregistrer(playerId, idSync, cache.omicronPlans(), cache.unitLibelleByBaseId(), cache.omicronOptionsByCle());
        omicronModeService.calculerEtEnregistrer(playerId, buildResult.skills(), definitions, idSync);
        stopWatch.stop();

        stopWatch.start("rosterUnitStatCalculService");
        String resultatStats = rosterUnitStatCalculService.calculerEtEnregistrer(playerId, unitesActuelles, modsActuels, cache.statReferentiel());
        stopWatch.stop();

        log.info("{} | Stats: {}", resultat, resultatStats);
        log.info("Bilan de performance :\n{}", stopWatch.prettyPrint());

        return resultat;
    }

    @Transactional
    public int historiserRosterActuel() {
        log.info("Début de la copie du roster actuel vers l'historique...");
        int nombreLignesCopiees = rosterUnitHistoriqueRepository.copierRosterActuelVersHistorique();
        log.info("Historisation terminée : {} unité(s) ajoutée(s) à l'historique.", nombreLignesCopiees);
        return nombreLignesCopiees;
    }

    /** Nettoyage propre à la guilde (l'externe a sa purge dédiée). */
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        log.info("Début du nettoyage des joueurs inactifs dans les tables '_actuel'...");

        if (joueursActifs == null || joueursActifs.isEmpty()) {
            log.warn("Aucun joueur actif fourni, annulation du nettoyage par sécurité.");
            return;
        }

        rosterUnitActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
        rosterUnitSkillActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
        rosterUnitModActuelRepository.deleteByPlayerIdNotIn(joueursActifs);

        rosterUnitActuelRepository.flush();
        rosterUnitSkillActuelRepository.flush();
        rosterUnitModActuelRepository.flush();

        log.info("Nettoyage des joueurs inactifs terminé.");
    }
}