package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.dto.pivot.UnitModDTO;
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
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
    private final RosterUnitModActuelRepository rosterUnitModActuelRepository;
    private final SkillDefinitionRepository skillDefinitionRepository;
    private final PlayerModQService playerModQService;
    private final RosterUnitStatCalculService rosterUnitStatCalculService;
    private final FarmPlanProgressService farmPlanProgressService;
    private final OmicronPlanProgressService omicronPlanProgressService;
    private final OmicronModeService omicronModeService;
    private final UnitSkillCalculationService unitSkillCalculationService;
    private final UnitModCalculationService unitModCalculationService;

    @Transactional
    public String enregistrerRoster(PlayerResponse response, Long idSync) {
        String playerId = response.playerId();
        List<PlayerResponse.RosterUnit> roster = response.rosterUnit();

        if (roster == null || roster.isEmpty()) {
            log.warn("Aucune unité dans le roster pour le joueur {}", playerId);
            return "Aucune unité trouvée";
        }

        // Référentiel zeta/omicron, chargé une fois pour toute la synchro
        Map<String, SkillDefinition> definitions = skillDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(SkillDefinition::getIdSkill, d -> d));

        // Suppression des anciennes données actuelles (avant de réinsérer les nouvelles)
        rosterUnitActuelRepository.deleteByPlayerId(playerId);
        rosterUnitSkillActuelRepository.deleteByPlayerId(playerId);
        rosterUnitModActuelRepository.deleteByPlayerId(playerId);

        // FORCER Hibernate à envoyer les DELETE immédiatement à la BDD
        rosterUnitActuelRepository.flush();
        rosterUnitSkillActuelRepository.flush();
        rosterUnitModActuelRepository.flush();

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

        // Calcul des skills : une seule fois pour tout le roster, en dehors de la boucle unités
        UnitSkillCalculationService.UnitSkillBuildResult buildResult =
                unitSkillCalculationService.construireSkillsDto(roster, definitions);

        int skillsSansDefinition = buildResult.skillsSansDefinition();

        List<RosterUnitSkillActuel> skillsActuels = buildResult.skills().stream()
                .map(dto -> RosterUnitSkillActuel.builder()
                        .playerId(playerId)
                        .idUnit(dto.idUnit())
                        .idSkill(dto.idSkill())
                        .tier(dto.tier())
                        .type(dto.type())
                        .numero(dto.numero())
                        .skillZeta(dto.skillZeta())
                        .zetaApplied(dto.zetaApplied())
                        .skillOmicron(dto.skillOmicron())
                        .omicronApplied(dto.omicronApplied())
                        .idSync(idSync)
                        .build())
                .collect(Collectors.toList());

        rosterUnitActuelRepository.saveAll(unitesActuelles);
        rosterUnitSkillActuelRepository.saveAll(skillsActuels);
        rosterUnitModActuelRepository.saveAll(modsActuels);

        if (skillsSansDefinition > 0) {
            log.warn("{} skill(s) sans correspondance dans skill_definition (référentiel pas encore synchronisé ?)",
                    skillsSansDefinition);
        }

        String resultat = String.format(
                "Sync #%d : %d unité(s), %d skill(s) (%d sans référentiel), %d ligne(s) de mod",
                idSync, unitesActuelles.size(), skillsActuels.size(), skillsSansDefinition, modsActuels.size());

        playerModQService.calculerEtEnregistrer(playerId, modsActuels, idSync);

        farmPlanProgressService.calculerEtEnregistrer(playerId, idSync);
        omicronPlanProgressService.calculerEtEnregistrer(playerId, idSync);
        omicronModeService.calculerEtEnregistrer(playerId, skillsActuels, definitions, idSync);

        String resultatStats = rosterUnitStatCalculService.calculerEtEnregistrer(playerId, unitesActuelles, modsActuels);

        log.info("{} | Stats: {}", resultat, resultatStats);
        return resultat;
    }
    
    @Transactional
    public int historiserRosterActuel() {
        log.info("Début de la copie du roster actuel vers l'historique...");
        int nombreLignesCopiees = rosterUnitHistoriqueRepository.copierRosterActuelVersHistorique();
        log.info("Historisation terminée : {} unité(s) ajoutée(s) à l'historique.", nombreLignesCopiees);
        return nombreLignesCopiees;
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        log.info("Début du nettoyage des joueurs inactifs dans les tables '_actuel'...");

        // Sécurité
        if (joueursActifs == null || joueursActifs.isEmpty()) {
            log.warn("Aucun joueur actif fourni, annulation du nettoyage par sécurité.");
            return;
        }

        // Supprimer les lignes orphelines
        rosterUnitActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
        rosterUnitSkillActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
        rosterUnitModActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
        
        // FORCER Hibernate à envoyer les DELETE
        rosterUnitActuelRepository.flush();
        rosterUnitSkillActuelRepository.flush();
        rosterUnitModActuelRepository.flush();

        log.info("Nettoyage des joueurs inactifs terminé.");
    }
}