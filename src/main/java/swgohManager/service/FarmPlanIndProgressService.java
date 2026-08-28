package swgohManager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.PlanFarmInd;
import swgohManager.model.PlayerPdfIndActuel;
import swgohManager.model.PlayerPdfIndHistorique;
import swgohManager.model.SyncExecution;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.PlanFarmIndRepository;
import swgohManager.repository.PlayerPdfIndActuelRepository;
import swgohManager.repository.PlayerPdfIndHistoriqueRepository;
import swgohManager.repository.RosterUnitActuelRepository;
import swgohManager.repository.SyncExecutionRepository;
import swgohManager.repository.UnitDefinitionRepository;

@Service
@RequiredArgsConstructor
public class FarmPlanIndProgressService {

    private final PlanFarmIndRepository planFarmIndRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final SyncExecutionRepository syncExecutionRepository;
    private final PlayerPdfIndActuelRepository playerPdfIndActuelRepository;
    private final PlayerPdfIndHistoriqueRepository playerPdfIndHistoriqueRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;

    public record DetailRow(Long id, String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint,Instant dateAjout) {}
    public record PlayerFarmIndProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PointProgression(Instant date, Double pourcentage) {}

    public PlayerFarmIndProgress getProgression(String playerId) {
        return calculer(playerId, planFarmIndRepository.findByPlayerId(playerId));
    }

    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync) {
        PlayerFarmIndProgress progress = calculer(playerId, planFarmIndRepository.findByPlayerId(playerId));

        PlayerPdfIndActuel existant = playerPdfIndActuelRepository.findByPlayerId(playerId).orElse(null);

        // Archivage en historique uniquement s'il s'agit d'une synchronisation API (idSync != null)
        if (existant != null && idSync != null) {
            playerPdfIndHistoriqueRepository.save(PlayerPdfIndHistorique.builder()
                    .playerId(existant.getPlayerId())
                    .atteint(existant.getAtteint())
                    .total(existant.getTotal())
                    .pourcentage(existant.getPourcentage())
                    .idSync(existant.getIdSync())
                    .build());
        } else if (existant == null) {
            existant = new PlayerPdfIndActuel();
            existant.setPlayerId(playerId);
        }

        // Mise à jour de la table "Actuel"
        existant.setAtteint(progress.atteint());
        existant.setTotal(progress.total());
        existant.setPourcentage(progress.pourcentage());
        if (idSync != null) {
            existant.setIdSync(idSync);
        }

        playerPdfIndActuelRepository.save(existant);
    }

    public Map<String, Double> getPourcentagesPourJoueurs(List<String> playerIds) {
        return playerPdfIndActuelRepository.findByPlayerIdIn(playerIds).stream()
                .filter(p -> p.getPourcentage() != null) // Évite le plantage avec Collectors.toMap
                .collect(Collectors.toMap(PlayerPdfIndActuel::getPlayerId, PlayerPdfIndActuel::getPourcentage));
    }

    public PlayerFarmIndProgress getProgressionPersistee(String playerId) {
        return playerPdfIndActuelRepository.findByPlayerId(playerId)
                .map(p -> new PlayerFarmIndProgress(
                        p.getAtteint() != null ? p.getAtteint() : 0,
                        p.getTotal() != null ? p.getTotal() : 0,
                        p.getPourcentage(),
                        List.of()))
                .orElse(new PlayerFarmIndProgress(0, 0, null, List.of()));
    }

    public List<PointProgression> getProgressionDansLeTempsPersistee(String playerId) {
        List<PlayerPdfIndHistorique> histo = playerPdfIndHistoriqueRepository.findByPlayerIdOrderByIdSyncAsc(playerId);
        Optional<PlayerPdfIndActuel> actuelOpt = playerPdfIndActuelRepository.findByPlayerId(playerId);

        Map<Long, Double> pourcentageParSync = new LinkedHashMap<>();
        for (PlayerPdfIndHistorique h : histo) {
            pourcentageParSync.put(h.getIdSync(), h.getPourcentage());
        }
        actuelOpt.ifPresent(a -> pourcentageParSync.put(a.getIdSync(), a.getPourcentage()));

        if (pourcentageParSync.isEmpty()) {
            return List.of();
        }

        Map<Long, Instant> dateParSync = new HashMap<>();
        for (SyncExecution se : syncExecutionRepository.findAllById(pourcentageParSync.keySet())) {
            dateParSync.put(se.getIdSync(), se.getDateSync());
        }

        return pourcentageParSync.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PointProgression(dateParSync.get(e.getKey()), e.getValue()))
                .toList();
    }

    private PlayerFarmIndProgress calculer(String playerId, List<PlanFarmInd> plans) {
        if (plans.isEmpty()) {
            return new PlayerFarmIndProgress(0, 0, null, List.of());
        }

        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));

        Map<String, RosterBaseIdProgressProjection> parBaseId = rosterUnitActuelRepository
                .findMaxEtoilesRelicByBaseId(playerId).stream()
                .collect(Collectors.toMap(RosterBaseIdProgressProjection::getBaseId, p -> p));

        List<DetailRow> details = new ArrayList<>();
        int atteint = 0;

        for (PlanFarmInd plan : plans) {
            RosterBaseIdProgressProjection p = parBaseId.get(plan.getBaseId());
            int etoilesActuelles = (p != null && p.getMaxEtoiles() != null) ? p.getMaxEtoiles() : 0;
            Integer relicActuel = (p != null) ? p.getMaxRelic() : null;
            int relicPourComparaison = (relicActuel != null) ? relicActuel : 0;

            boolean ok = etoilesActuelles >= plan.getEtoilesCible() && relicPourComparaison >= plan.getRelicCible();
            if (ok) atteint++;

            String nomUnite = unitMap.getOrDefault(plan.getBaseId(), plan.getBaseId());
            details.add(new DetailRow(plan.getId(), plan.getBaseId(), nomUnite, plan.getEtoilesCible(), plan.getRelicCible(), relicActuel, ok,plan.getDateAjout()));
        }

        double pourcentage = 100.0 * atteint / plans.size();
        return new PlayerFarmIndProgress(atteint, plans.size(), pourcentage, details);
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
        	planFarmIndRepository.deleteByPlayerIdNotIn(joueursActifs);
        }
    }
}