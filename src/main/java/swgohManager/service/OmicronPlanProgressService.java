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
        PlayerOmicronProgress progress = getProgression(playerId);

        // On boucle sur les 4 priorités pour les enregistrer
        for (int i = 1; i <= 4; i++) {
            PrioriteSummary pSummary = progress.parPriorite().get(i);
            String prioriteLabel = "P" + i;
            
            int atteint = pSummary != null ? pSummary.atteint() : 0;
            int total = pSummary != null ? pSummary.total() : 0;
            Double pourcentage = pSummary != null ? pSummary.pourcentage() : null;

            PlayerPdfOmicronActuel existant = playerPdfOmicronActuelRepository
                    .findByPlayerIdAndPriorite(playerId, prioriteLabel).orElse(null);

            if (existant != null) {
                // Historisation
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
                existant.setPriorite(prioriteLabel);
            }

            existant.setAtteint(atteint);
            existant.setTotal(total);
            existant.setPourcentage(pourcentage);
            existant.setIdSync(idSync);

            playerPdfOmicronActuelRepository.save(existant);
        }
    }

    // Renvoie désormais une Map contenant le playerId, et à l'intérieur une autre Map avec les 4 pourcentages
    public Map<String, Map<String, Double>> getPourcentagesOmiPourJoueurs(List<String> playerIds) {
        List<PlayerPdfOmicronActuel> tousLesActuels = playerPdfOmicronActuelRepository.findByPlayerIdIn(playerIds);
        
        // Initialiser la map pour chaque joueur
        Map<String, Map<String, Double>> mapGlobale = new HashMap<>();
        for (String pid : playerIds) {
            mapGlobale.put(pid, new HashMap<>());
        }
        
        // Remplir avec les données en base
        for (PlayerPdfOmicronActuel p : tousLesActuels) {
            Double pct = p.getPourcentage() != null ? p.getPourcentage() : -1.0;
            mapGlobale.get(p.getPlayerId()).put(p.getPriorite(), pct);
        }
        
        return mapGlobale;
    }
    
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            playerPdfOmicronActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            playerPdfOmicronActuelRepository.flush(); // Force l'exécution immédiate
        }
    }
}