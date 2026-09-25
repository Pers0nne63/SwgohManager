package swgohManager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.PlayerOmicronStatusProjection;
import swgohManager.model.OmicronPlan;
import swgohManager.model.PlayerPdfOmicronActuel;
import swgohManager.model.PlayerPdfOmicronHistorique;
import swgohManager.repository.ExternalRosterUnitSkillActuelRepository;
import swgohManager.repository.OmicronPlanRepository;
import swgohManager.repository.PlayerPdfOmicronActuelRepository;
import swgohManager.repository.PlayerPdfOmicronHistoriqueRepository;
import swgohManager.repository.RosterUnitSkillActuelRepository;

@Service
@RequiredArgsConstructor
public class OmicronPlanProgressService {

    private final OmicronPlanRepository omicronPlanRepository;
    private final RosterUnitSkillActuelRepository rosterUnitSkillActuelRepository;
    private final ExternalRosterUnitSkillActuelRepository externalRosterUnitSkillActuelRepository;
    private final PlayerPdfOmicronActuelRepository playerPdfOmicronActuelRepository;
    private final PlayerPdfOmicronHistoriqueRepository playerPdfOmicronHistoriqueRepository;
    private final OmicronPlanCalculationService omicronPlanCalculationService;

    private List<PlayerOmicronStatusProjection> statutRows(String playerId, Portee portee) {
        return portee == Portee.GUILDE
                ? rosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId)
                : externalRosterUnitSkillActuelRepository.findStatutOmicronParJoueur(playerId);
    }

    public OmicronPlanCalculationService.PlayerOmicronProgress getProgression(String playerId, Portee portee) {
        List<OmicronPlan> plans = omicronPlanRepository.findAll();
        return omicronPlanCalculationService.calculerProgression(plans, statutRows(playerId, portee));
    }

    public OmicronPlanCalculationService.GlobalSummary getGlobalProgression(String playerId, Portee portee) {
        return omicronPlanCalculationService.calculerGlobalProgression(getProgression(playerId, portee));
    }

    public List<OmicronPlanCalculationService.OmicronColonneDetail> getColonnesDetail() {
        return omicronPlanCalculationService.getColonnesDetail(omicronPlanRepository.findAll());
    }

    /** Si une clé est absente de la map, le joueur ne possède pas le personnage (ou pas ce skill). */
    public Map<String, Boolean> getStatutDetailParJoueur(String playerId, Portee portee) {
        return omicronPlanCalculationService.statutParCle(statutRows(playerId, portee));
    }

    /** Persistance avec historique — réservée à la guilde, pas d'équivalent externe pour l'instant. */
    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync) {
        OmicronPlanCalculationService.PlayerOmicronProgress progress = getProgression(playerId, Portee.GUILDE);
        persister(playerId, idSync, progress);
    }

    /** Variante rapide : plans/unitMap/optionsMap déjà chargés une fois pour tout le lot (synchro de masse). */
    @Transactional
    public void calculerEtEnregistrer(String playerId, Long idSync, List<OmicronPlan> plans,
            Map<String, String> unitMap, Map<String, OmicronPlanService.Option> optionsMap) {
        OmicronPlanCalculationService.PlayerOmicronProgress progress = omicronPlanCalculationService.calculerProgression(
                plans, statutRows(playerId, Portee.GUILDE), unitMap, optionsMap);
        persister(playerId, idSync, progress);
    }

    private void persister(String playerId, Long idSync, OmicronPlanCalculationService.PlayerOmicronProgress progress) {
    	Map<String, PlayerPdfOmicronActuel> existants = playerPdfOmicronActuelRepository
    	        .findAllByPlayerId(playerId).stream()
    	        .collect(Collectors.toMap(PlayerPdfOmicronActuel::getPriorite, e -> e));

        List<PlayerPdfOmicronHistorique> aHistoriser = new ArrayList<>();
        List<PlayerPdfOmicronActuel> aSauver = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            String prioriteLabel = "P" + i;
            OmicronPlanCalculationService.PrioriteSummary pSummary = progress.parPriorite().get(i);
            int atteint = pSummary != null ? pSummary.atteint() : 0;
            int total = pSummary != null ? pSummary.total() : 0;
            Double pourcentage = pSummary != null ? pSummary.pourcentage() : null;

            PlayerPdfOmicronActuel existant = existants.get(prioriteLabel);
            if (existant != null) {
                aHistoriser.add(PlayerPdfOmicronHistorique.builder()
                        .playerId(existant.getPlayerId()).priorite(existant.getPriorite())
                        .atteint(existant.getAtteint()).total(existant.getTotal())
                        .pourcentage(existant.getPourcentage()).idSync(existant.getIdSync())
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
            aSauver.add(existant);
        }

        playerPdfOmicronHistoriqueRepository.saveAll(aHistoriser);
        playerPdfOmicronActuelRepository.saveAll(aSauver);
    }

    public Map<String, Map<String, Double>> getPourcentagesOmiPourJoueurs(List<String> playerIds) {
        List<PlayerPdfOmicronActuel> tousLesActuels = playerPdfOmicronActuelRepository.findByPlayerIdIn(playerIds);

        Map<String, Map<String, Double>> mapGlobale = new HashMap<>();
        for (String pid : playerIds) {
            mapGlobale.put(pid, new HashMap<>());
        }

        for (PlayerPdfOmicronActuel p : tousLesActuels) {
            Double pct = p.getPourcentage() != null ? p.getPourcentage() : -1.0;
            mapGlobale.get(p.getPlayerId()).put(p.getPriorite(), pct);
        }

        return mapGlobale;
    }

    /** Nettoyage propre à la guilde (l'externe a sa purge dédiée). */
    @Transactional
    public void nettoyerJoueursInactifs(List<String> joueursActifs) {
        if (!joueursActifs.isEmpty()) {
            playerPdfOmicronActuelRepository.deleteByPlayerIdNotIn(joueursActifs);
            playerPdfOmicronActuelRepository.flush();
        }
    }
}