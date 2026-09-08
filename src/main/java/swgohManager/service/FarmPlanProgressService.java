package swgohManager.service;

import swgohManager.controller.dto.RosterBaseIdProgressProjection;
import swgohManager.model.PlayerPdfActuel;
import swgohManager.model.PlayerPdfHistorique;
import swgohManager.model.SyncExecution;
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
    private final SyncExecutionRepository syncExecutionRepository;
    private final PlayerPdfActuelRepository playerPdfActuelRepository;
    private final PlayerPdfHistoriqueRepository playerPdfHistoriqueRepository;
    private final FarmPlanCalculationService farmPlanCalculationService;

    public record DetailRow(String baseId, String nomUnite, Integer etoilesCible, Integer relicCible, Integer relicActuel, boolean atteint) {}
    public record PlayerFarmProgress(int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PointProgression(Instant date, Double pourcentage) {}

    public PlayerFarmProgress getProgression(String playerId) {
        return convertir(calculer(playerId));
    }

    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync) {
        PlayerFarmProgress progress = convertir(calculer(playerId));

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

    private FarmPlanCalculationService.FarmProgress calculer(String playerId) {
        List<RosterBaseIdProgressProjection> rosterProgress = rosterUnitActuelRepository.findMaxEtoilesRelicByBaseId(playerId);
        return farmPlanCalculationService.calculer(farmPlanRepository.findAll(), rosterProgress);
    }

    private PlayerFarmProgress convertir(FarmPlanCalculationService.FarmProgress p) {
        List<DetailRow> details = p.details().stream()
                .map(d -> new DetailRow(d.baseId(), d.nomUnite(), d.etoilesCible(), d.relicCible(), d.relicActuel(), d.atteint()))
                .toList();
        return new PlayerFarmProgress(p.atteint(), p.total(), p.pourcentage(), details);
    }

    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            playerPdfActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            playerPdfActuelRepository.flush();
        }
    }

}