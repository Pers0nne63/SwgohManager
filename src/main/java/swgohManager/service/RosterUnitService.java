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
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitModActuel;
import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.model.SkillDefinition;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.RosterUnitHistoriqueRepository;
import swgohManager.repository.RosterUnitModActuelRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import swgohManager.repository.SkillDefinitionRepository;
import swgohManager.util.SkillIdParser;
import swgohManager.repository.JoueurRepository;
import swgohManager.model.Joueur;

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
    private final JoueurRepository joueurRepository;

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
        List<RosterUnitSkillActuel> skillsActuels = new ArrayList<>();
        List<RosterUnitModActuel> modsActuels = new ArrayList<>();

        int skillsSansDefinition = 0;

        for (PlayerResponse.RosterUnit u : roster) {
            Integer relicValue = u.relic() != null && u.relic().currentTier() != null
                    ? u.relic().currentTier() - 2 : null;

            unitesActuelles.add(RosterUnitActuel.builder()
                    .playerId(playerId).idUnit(u.id()).definitionId(u.definitionId())
                    .etoiles(u.currentRarity()).niveau(u.currentLevel()).gear(u.currentTier()).relic(relicValue)
                    .idSync(idSync)
                    .build());

            if (u.skill() != null) {
                for (PlayerResponse.Skill s : u.skill()) {
                    SkillIdParser.ParsedSkillId parsed = SkillIdParser.parse(s.id());
                    SkillDefinition def = definitions.get(s.id());

                    Boolean skillZeta = null, skillOmicron = null, zetaApplied = null, omicronApplied = null;

                    if (def != null) {
                        skillZeta = def.getSkillZeta();
                        skillOmicron = def.getSkillOmicron();

                        zetaApplied = Boolean.TRUE.equals(skillZeta)
                                && def.getTierZetaRequis() != null
                                && s.tier() != null
                                && s.tier() >= def.getTierZetaRequis();

                        omicronApplied = Boolean.TRUE.equals(skillOmicron)
                                && def.getTierOmicronRequis() != null
                                && s.tier() != null
                                && s.tier() >= (def.getTierOmicronRequis()-1);
                    } else {
                        skillsSansDefinition++;
                    }

                    skillsActuels.add(RosterUnitSkillActuel.builder()
                            .playerId(playerId).idUnit(u.id()).idSkill(s.id())
                            .tier(s.tier()).type(parsed.type()).numero(parsed.numero())
                            .skillZeta(skillZeta).zetaApplied(zetaApplied)
                            .skillOmicron(skillOmicron).omicronApplied(omicronApplied)
                            .idSync(idSync)
                            .build());
                }
            }

            if (u.equippedStatMod() != null) {
                for (PlayerResponse.EquippedStatMod mod : u.equippedStatMod()) {
                    modsActuels.addAll(construireLignesMod(playerId, u.id(), mod, idSync));
                }
            }
        }

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

    private List<RosterUnitModActuel> construireLignesMod(String playerId, String idUnit, PlayerResponse.EquippedStatMod mod, Long idSync) {
        List<RosterUnitModActuel> lignes = new ArrayList<>();

        String definitionId = mod.definitionId();
        String set = definitionId != null && definitionId.length() >= 1 ? definitionId.substring(0, 1) : null;
        String rarity = definitionId != null && definitionId.length() >= 2 ? definitionId.substring(1, 2) : null;
        String position = definitionId != null && definitionId.length() >= 3 ? definitionId.substring(2, 3) : null;

        Integer idPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? mod.primaryStat().stat().unitStatId() : null;
        Long valeurPrimaire = mod.primaryStat() != null && mod.primaryStat().stat() != null
                ? parseLong(mod.primaryStat().stat().unscaledDecimalValue()) : null;

        List<PlayerResponse.SecondaryStat> secondaires = mod.secondaryStat() != null
                ? mod.secondaryStat() : List.of();

        int ordre = 1;
        for (PlayerResponse.SecondaryStat secondaryStat : secondaires) {
            Integer idSecondaire = secondaryStat.stat() != null ? secondaryStat.stat().unitStatId() : null;
            Long valeurSecondaire = secondaryStat.stat() != null
                    ? parseLong(secondaryStat.stat().unscaledDecimalValue()) : null;

            lignes.add(RosterUnitModActuel.builder()
                    .playerId(playerId).idUnit(idUnit).idMod(mod.id()).definitionId(definitionId)
                    .set(set).rarity(rarity).position(position).niveau(mod.level())
                    .idPrimaire(idPrimaire).valeurPrimaire(valeurPrimaire)
                    .idSecondaire(idSecondaire).valeurSecondaire(valeurSecondaire)
                    .ordreSecondaire(ordre)
                    .idSync(idSync)
                    .build());

            ordre++;
        }

        return lignes;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique du mod : {}", value);
            return null;
        }
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