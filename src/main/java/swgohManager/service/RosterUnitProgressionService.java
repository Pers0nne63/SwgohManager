package swgohManager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.dto.pivot.UnitSkillDTO;
import swgohManager.model.RosterUnitActuel;
import swgohManager.model.RosterUnitProgression;
import swgohManager.model.RosterUnitSkillActuel;
import swgohManager.repository.RosterUnitProgressionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RosterUnitProgressionService {

    private final RosterUnitProgressionRepository progressionRepository;

    @Transactional
    public void detecterEtEnregistrer(String playerId, List<RosterUnitActuel> anciennesUnites,
            List<RosterUnitActuel> nouvellesUnites, Long idSync, Set<String> baseIdsEra) {

        log.info("detecterEtEnregistrer appelé pour playerId={} idSync={} thread={} anciennesUnites.size={} nouvellesUnites.size={}",
                playerId, idSync, Thread.currentThread().getName(), anciennesUnites.size(), nouvellesUnites.size());

        if (anciennesUnites == null || anciennesUnites.isEmpty()) {
            return;
        }

        // NOUVEAU : Purge des progressions déjà générées pour cette même passe/idSync
        progressionRepository.deleteByPlayerIdAndIdSync(playerId, idSync);
        progressionRepository.flush();

        // Map sécurisé contre les idUnit null et les doublons de clés éventuels
        Map<String, RosterUnitActuel> anciennesParIdUnit = anciennesUnites.stream()
                .filter(u -> u.getIdUnit() != null)
                .collect(Collectors.toMap(
                        RosterUnitActuel::getIdUnit, 
                        u -> u, 
                        (existant, remplaçant) -> existant
                ));

        Instant maintenant = Instant.now();
        List<RosterUnitProgression> progressions = new ArrayList<>();

        for (RosterUnitActuel nouvelle : nouvellesUnites) {
            if (nouvelle.getIdUnit() == null || estUniteEra(nouvelle.getDefinitionId(), baseIdsEra)) {
                continue;
            }

            RosterUnitActuel ancienne = anciennesParIdUnit.get(nouvelle.getIdUnit());

            if (ancienne == null) {
                progressions.add(RosterUnitProgression.builder()
                        .playerId(playerId).idUnit(nouvelle.getIdUnit()).definitionId(nouvelle.getDefinitionId())
                        .dateConstat(maintenant).idSync(idSync)
                        .nouvelleUnite(true)
                        .etoilesApres(nouvelle.getEtoiles())
                        .gearApres(nouvelle.getGear())
                        .relicApres(nouvelle.getRelic())
                        .build());
                continue;
            }

            boolean change = !Objects.equals(ancienne.getEtoiles(), nouvelle.getEtoiles())
                    || !Objects.equals(ancienne.getGear(), nouvelle.getGear())
                    || !Objects.equals(ancienne.getRelic(), nouvelle.getRelic());

            if (change) {
                progressions.add(RosterUnitProgression.builder()
                        .playerId(playerId).idUnit(nouvelle.getIdUnit()).definitionId(nouvelle.getDefinitionId())
                        .dateConstat(maintenant).idSync(idSync)
                        .nouvelleUnite(false)
                        .etoilesAvant(ancienne.getEtoiles())
                        .etoilesApres(nouvelle.getEtoiles())
                        .gearAvant(ancienne.getGear())
                        .gearApres(nouvelle.getGear())
                        .relicAvant(ancienne.getRelic())
                        .relicApres(nouvelle.getRelic())
                        .build());
            }
        }

        if (!progressions.isEmpty()) {
            progressionRepository.saveAll(progressions);
            log.info("{} évolution(s) détectée(s) pour {}", progressions.size(), playerId);
        }
    }

    @Transactional
    public void detecterOmicronsEtEnregistrer(String playerId, List<RosterUnitSkillActuel> anciennesSkills,
            List<UnitSkillDTO> nouvellesSkills, Map<String, String> definitionIdParIdUnit,
            Long idSync, Set<String> baseIdsEra) {

        if (anciennesSkills == null || anciennesSkills.isEmpty()) {
            return;
        }

        Set<String> anciennesOmicronsAppliques = anciennesSkills.stream()
                .filter(s -> Boolean.TRUE.equals(s.getOmicronApplied()) && s.getIdUnit() != null && s.getIdSkill() != null)
                .map(s -> cleSkill(s.getIdUnit(), s.getIdSkill()))
                .collect(Collectors.toSet());

        Instant maintenant = Instant.now();
        List<RosterUnitProgression> progressions = new ArrayList<>();

        for (UnitSkillDTO skill : nouvellesSkills) {
            if (!Boolean.TRUE.equals(skill.omicronApplied()) || skill.idUnit() == null || skill.idSkill() == null) {
                continue;
            }

            String cle = cleSkill(skill.idUnit(), skill.idSkill());
            if (anciennesOmicronsAppliques.contains(cle)) continue;

            String definitionId = definitionIdParIdUnit.get(skill.idUnit());
            if (estUniteEra(definitionId, baseIdsEra)) continue;

            progressions.add(RosterUnitProgression.builder()
                    .playerId(playerId)
                    .idUnit(skill.idUnit())
                    .definitionId(definitionId)
                    .idSkill(skill.idSkill())
                    .skillType(skill.type())
                    .skillNumero(skill.numero())
                    .dateConstat(maintenant)
                    .idSync(idSync)
                    .omicronObtenu(true)
                    .build());
        }

        if (!progressions.isEmpty()) {
            progressionRepository.saveAll(progressions);
            log.info("{} omicron(s) nouvellement obtenu(s) pour {}", progressions.size(), playerId);
        }
    }

    private String cleSkill(String idUnit, String idSkill) {
        return idUnit + "|" + idSkill;
    }

    private boolean estUniteEra(String definitionId, Set<String> baseIdsEra) {
        if (definitionId == null || baseIdsEra == null || baseIdsEra.isEmpty()) return false;
        String baseId = definitionId.contains(":") ? definitionId.split(":", 2)[0] : definitionId;
        return baseIdsEra.contains(baseId);
    }
}