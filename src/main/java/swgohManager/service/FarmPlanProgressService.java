package swgohManager.service;

import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.FarmPlan;
import swgohManager.model.PlayerPdfActuel;
import swgohManager.model.PlayerPdfHistorique;
import swgohManager.model.SyncExecution;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmPlanProgressService {

    private final FarmPlanRepository farmPlanRepository;
    private final RosterUnitActuelRepository rosterUnitActuelRepository;
    private final RosterUnitHistoriqueRepository rosterUnitHistoriqueRepository;
    private final SyncExecutionRepository syncExecutionRepository;
    private final PlayerPdfActuelRepository playerPdfActuelRepository;
    private final PlayerPdfHistoriqueRepository playerPdfHistoriqueRepository;
    private final UnitDefinitionRepository unitDefinitionRepository; // 👈 Ajout

    public record DetailRow(String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}
    public record PlayerFarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PointProgression(Instant date, Double pourcentage) {}

    public PlayerFarmProgress getProgression(String playerId) {
        return calculer(playerId, farmPlanRepository.findAll());
    }

    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync) {
        PlayerFarmProgress progress = calculer(playerId, farmPlanRepository.findAll());

        PlayerPdfActuel existant = playerPdfActuelRepository.findByPlayerId(playerId).orElse(null);

        if (existant != null) {
            playerPdfHistoriqueRepository.save(PlayerPdfHistorique.builder()
                    .playerId(existant.getPlayerId())
                    .atteint(existant.getAtteint())
                    .total(existant.getTotal())
                    .pourcentage(existant.getPourcentage())
                    .idSync(existant.getIdSync())
                    .build());
        } else {
            existant = new PlayerPdfActuel();
            existant.setPlayerId(playerId);
        }

        existant.setAtteint(progress.atteint());
        existant.setTotal(progress.total());
        existant.setPourcentage(progress.pourcentage());
        existant.setIdSync(idSync);

        playerPdfActuelRepository.save(existant);
    }

    public Map<String, Double> getPourcentagesPourJoueurs(List<String> playerIds) {
        return playerPdfActuelRepository.findByPlayerIdIn(playerIds).stream()
                .collect(Collectors.toMap(PlayerPdfActuel::getPlayerId, PlayerPdfActuel::getPourcentage));
    }

    public PlayerFarmProgress getProgressionPersistee(String playerId) {
        return playerPdfActuelRepository.findByPlayerId(playerId)
                .map(p -> new PlayerFarmProgress(
                        p.getAtteint() != null ? p.getAtteint() : 0,
                        p.getTotal() != null ? p.getTotal() : 0,
                        p.getPourcentage(),
                        List.of()))
                .orElse(new PlayerFarmProgress(0, 0, null, List.of()));
    }

    public List<PointProgression> getProgressionDansLeTempsPersistee(String playerId) {
        List<PlayerPdfHistorique> histo = playerPdfHistoriqueRepository.findByPlayerIdOrderByIdSyncAsc(playerId);
        Optional<PlayerPdfActuel> actuelOpt = playerPdfActuelRepository.findByPlayerId(playerId);

        Map<Long, Double> pourcentageParSync = new LinkedHashMap<>();
        for (PlayerPdfHistorique h : histo) {
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

    private PlayerFarmProgress calculer(String playerId, List<FarmPlan> plans) {
        if (plans.isEmpty()) {
            return new PlayerFarmProgress(0, 0, null, List.of());
        }

        // Map baseId -> Libellé lisible du personnage (dédupliqué)
        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));

        Map<String, RosterBaseIdProgressProjection> parBaseId = rosterUnitActuelRepository
                .findMaxEtoilesRelicByBaseId(playerId).stream()
                .collect(Collectors.toMap(RosterBaseIdProgressProjection::getBaseId, p -> p));

        List<DetailRow> details = new ArrayList<>();
        int atteint = 0;

        for (FarmPlan plan : plans) {
            RosterBaseIdProgressProjection p = parBaseId.get(plan.getBaseId());
            int etoilesActuelles = (p != null && p.getMaxEtoiles() != null) ? p.getMaxEtoiles() : 0;
            Integer relicActuel = (p != null) ? p.getMaxRelic() : null;
            int relicPourComparaison = (relicActuel != null) ? relicActuel : 0;

            boolean ok = etoilesActuelles >= plan.getEtoilesCible() && relicPourComparaison >= plan.getRelicCible();
            if (ok) atteint++;

            String nomUnite = unitMap.getOrDefault(plan.getBaseId(), plan.getBaseId());
            details.add(new DetailRow(plan.getBaseId(), nomUnite, plan.getEtoilesCible(), plan.getRelicCible(), relicActuel, ok));
        }

        double pourcentage = 100.0 * atteint / plans.size();
        return new PlayerFarmProgress(atteint, plans.size(), pourcentage, details);
    }
}