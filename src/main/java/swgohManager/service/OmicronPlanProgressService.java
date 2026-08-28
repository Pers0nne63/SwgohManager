package swgohManager.service;

import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.model.PlayerPdfOmicronActuel;
import swgohManager.model.PlayerPdfOmicronHistorique;
import swgohManager.model.UnitDefinition;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.PlayerPdfOmicronActuelRepository;
import swgohManager.repository.PlayerPdfOmicronHistoriqueRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;
import swgohManager.repository.UnitDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OmicronPlanProgressService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;
    private final OmicronPlanService omicronPlanService;
    private final PlayerPdfOmicronActuelRepository playerPdfOmicronActuelRepository;
    private final PlayerPdfOmicronHistoriqueRepository playerPdfOmicronHistoriqueRepository;
    private final UnitDefinitionRepository unitDefinitionRepository; // 👈 Ajout

    public record DetailRow(String baseId, String label, boolean atteint) {}
    public record PrioriteSummary(int priorite, int atteint, int total, Double pourcentage, List<DetailRow> details) {}
    public record PlayerOmicronProgress(Map<Integer, PrioriteSummary> parPriorite) {}
    public record GlobalSummary(String label, int atteint, int total, Double pourcentage) {}

    public PlayerOmicronProgress getProgression(String playerId) {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();

        // Map baseId -> Libellé lisible du personnage
        Map<String, String> unitMap = unitDefinitionRepository.findAll().stream()
                .filter(u -> u.getBaseId() != null && u.getLibelle() != null)
                .collect(Collectors.toMap(UnitDefinition::getBaseId, UnitDefinition::getLibelle, (v1, v2) -> v1));

        Map<String, OmicronPlanService.Option> optionsParBaseIdSkill = new HashMap<>();
        for (OmicronPlanService.Option o : omicronPlanService.getOptionsDisponibles()) {
            optionsParBaseIdSkill.put(o.baseId() + "|" + o.idSkill(), o);
        }

        Map<String, Boolean> statutJoueur = new HashMap<>();
        for (PlayerOmicronStatusProjection p : rosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId)) {
            statutJoueur.put(p.getBaseId() + "|" + p.getIdSkill(), Boolean.TRUE.equals(p.getOmicronApplied()));
        }

        Map<Integer, List<OmicronPlan>> parPrioriteBrut = new TreeMap<>();
        for (OmicronPlan plan : plans) {
            int p = plan.getPriorite() != null ? plan.getPriorite() : 0;
            parPrioriteBrut.computeIfAbsent(p, k -> new ArrayList<>()).add(plan);
        }

        Map<Integer, PrioriteSummary> resultat = new LinkedHashMap<>();
        for (int priorite = 1; priorite <= 4; priorite++) {
            List<OmicronPlan> lignes = parPrioriteBrut.getOrDefault(priorite, List.of());
            List<DetailRow> details = new ArrayList<>();
            int atteint = 0;

            for (OmicronPlan plan : lignes) {
                String cle = plan.getBaseId() + "|" + plan.getIdSkill();
                boolean ok = Boolean.TRUE.equals(statutJoueur.get(cle));
                if (ok) atteint++;

                String nomUnite = unitMap.getOrDefault(plan.getBaseId(), plan.getBaseId());
                OmicronPlanService.Option option = optionsParBaseIdSkill.get(cle);
                
                // Remplace le baseId brut par le nom français du personnage s'il apparaît dans le libellé
                String label = option != null ? option.label().replace(plan.getBaseId(), nomUnite) : nomUnite;
                details.add(new DetailRow(plan.getBaseId(), label, ok));
            }

            Double pourcentage = lignes.isEmpty() ? null : 100.0 * atteint / lignes.size();
            resultat.put(priorite, new PrioriteSummary(priorite, atteint, lignes.size(), pourcentage, details));
        }

        return new PlayerOmicronProgress(resultat);
    }

    public GlobalSummary getGlobalProgression(String playerId) {
        PlayerOmicronProgress progress = getProgression(playerId);
        int totalAtteint = 0;
        int totalTotal = 0;

        for (PrioriteSummary ps : progress.parPriorite().values()) {
            totalAtteint += ps.atteint();
            totalTotal += ps.total();
        }

        Double pct = totalTotal > 0 ? (100.0 * totalAtteint / totalTotal) : null;
        return new GlobalSummary("GLOBAL", totalAtteint, totalTotal, pct);
    }

    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync) {
        PrioriteSummary p1 = getProgression(playerId).parPriorite().get(1);

        int atteint = p1 != null ? p1.atteint() : 0;
        int total = p1 != null ? p1.total() : 0;
        Double pourcentage = p1 != null ? p1.pourcentage() : null;

        PlayerPdfOmicronActuel existant = playerPdfOmicronActuelRepository.findByPlayerId(playerId).orElse(null);

        if (existant != null) {
            playerPdfOmicronHistoriqueRepository.save(PlayerPdfOmicronHistorique.builder()
                    .playerId(existant.getPlayerId())
                    .priorite(existant.getPriorite())
                    .atteint(existant.getAtteint())
                    .total(existant.getTotal())
                    .pourcentage(existant.getPourcentage())
                    .idSync(existant.getIdSync())
                    .build());
        } else {
            existant = new PlayerPdfOmicronActuel();
            existant.setPlayerId(playerId);
        }

        existant.setPriorite("P1");
        existant.setAtteint(atteint);
        existant.setTotal(total);
        existant.setPourcentage(pourcentage);
        existant.setIdSync(idSync);

        playerPdfOmicronActuelRepository.save(existant);
    }

    public Map<String, Double> getPourcentagesOmiPourJoueurs(List<String> playerIds) {
        return playerPdfOmicronActuelRepository.findByPlayerIdIn(playerIds).stream()
                .collect(Collectors.toMap(
                        PlayerPdfOmicronActuel::getPlayerId,
                        p -> p.getPourcentage() != null ? p.getPourcentage() : -1.0,
                        (v1, v2) -> v1
                ));
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            playerPdfOmicronActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            playerPdfOmicronActuelRepository.flush(); // Force l'exécution immédiate
        }
    }
}