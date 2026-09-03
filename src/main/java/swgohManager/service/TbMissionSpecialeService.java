package swgohManager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.TbMissionHistoriqueProjection;
import swgohManager.controller.dto.TbMissionJoueurStatsProjection;
import swgohManager.controller.dto.TbParticipantProjection;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TbScoreJoueurRepository;
import swgohManager.repository.TerritoryBattleRepository;

@Service
@RequiredArgsConstructor
public class TbMissionSpecialeService {

    private final TbScoreJoueurRepository tbScoreJoueurRepository;
    private final TerritoryBattleRepository territoryBattleRepository;

    public enum StatutMission { REUSSI, ECHOUE, NON_TENTE }

    public enum MissionSpeciale {
        QIRA("Qira", "covert_round_attempted_mission_tb3_mixed_phase01_conflict03_covert01", "covert_complete_mission_tb3_mixed_phase01_conflict03_covert01"),
        JKCK("JKCK", "covert_round_attempted_mission_tb3_mixed_phase02_conflict01_covert01", "covert_complete_mission_tb3_mixed_phase02_conflict01_covert01"),
        SAW("Saw", "covert_round_attempted_mission_tb3_mixed_phase03_conflict01_covert01", "covert_complete_mission_tb3_mixed_phase03_conflict01_covert01"),
        REVA("Reva", "covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert01", "covert_complete_mission_tb3_mixed_phase03_conflict03_covert01"),
        BKM("BKM", "covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert02", "covert_complete_mission_tb3_mixed_phase03_conflict03_covert02"),
        MERRIN("Merrin", "covert_round_attempted_mission_tb3_mixed_phase03_conflict02_covert01", "covert_complete_mission_tb3_mixed_phase03_conflict02_covert01"),
        CLONES("Clones", "covert_round_attempted_mission_tb3_mixed_phase03_conflict01_bonus_covert01", "covert_complete_mission_tb3_mixed_phase03_conflict01_bonus_covert01"),
        INQUIS("Inquis", "covert_round_attempted_mission_tb3_mixed_phase04_conflict02_covert01", "covert_complete_mission_tb3_mixed_phase04_conflict02_covert01"),
        L337("L3-37", "covert_round_attempted_mission_tb3_mixed_phase04_conflict03_covert01", "covert_complete_mission_tb3_mixed_phase04_conflict03_covert01"),
        YHAN("YHan", "covert_round_attempted_mission_tb3_mixed_phase05_conflict03_covert01", "covert_complete_mission_tb3_mixed_phase05_conflict03_covert01");

        private final String libelle;
        private final String mapStatIdAttempted;
        private final String mapStatIdCompleted;

        MissionSpeciale(String libelle, String mapStatIdAttempted, String mapStatIdCompleted) {
            this.libelle = libelle;
            this.mapStatIdAttempted = mapStatIdAttempted;
            this.mapStatIdCompleted = mapStatIdCompleted;
        }

        public String getLibelle() { return libelle; }
        public String getMapStatIdAttempted() { return mapStatIdAttempted; }
        public String getMapStatIdCompleted() { return mapStatIdCompleted; }
    }

    public record JoueurHistoriqueMission(
            String playerId,
            String playerName,
            StatutMission statutActuel,
            List<StatutMission> historique
    ) {}

    public record MissionSpecialeAnalyse(
            long nbReussis,
            long nbEchecs,
            long nbNonTentes,
            List<TerritoryBattle> tbHistorique,
            List<JoueurHistoriqueMission> joueursEnEchec,
            List<JoueurHistoriqueMission> joueursNonTentes
    ) {}

    public MissionSpecialeAnalyse analyser(Long tbId, MissionSpeciale mission) {
    	Map<String, String> nomJoueurs = tbScoreJoueurRepository.findParticipantsTb(tbId).stream()
    	        .collect(Collectors.toMap(TbParticipantProjection::getPlayerId, TbParticipantProjection::getPlayerName, (a, b) -> a));

        Map<String, TbMissionJoueurStatsProjection> statsParJoueur = tbScoreJoueurRepository
                .findStatsMissionParTb(tbId, mission.getMapStatIdAttempted(), mission.getMapStatIdCompleted())
                .stream()
                .collect(Collectors.toMap(TbMissionJoueurStatsProjection::getPlayerId, s -> s, (a, b) -> a));

        List<Long> tbIdsPrecedentes = tbScoreJoueurRepository.findTbIdsPrecedentes(tbId);

        Map<String, List<TbMissionHistoriqueProjection>> historiqueParJoueur = tbIdsPrecedentes.isEmpty()
                ? Map.of()
                : tbScoreJoueurRepository
                    .findStatsMissionHistorique(tbIdsPrecedentes, mission.getMapStatIdAttempted(), mission.getMapStatIdCompleted())
                    .stream()
                    .collect(Collectors.groupingBy(TbMissionHistoriqueProjection::getPlayerId));

        // tbIdsPrecedentes est trié du plus récent au plus ancien -> on veut l'ordre chronologique (le plus ancien en premier)
        List<Long> ordreChrono = new ArrayList<>(tbIdsPrecedentes);
        Collections.reverse(ordreChrono);

        Map<Long, TerritoryBattle> tbParId = territoryBattleRepository.findAllById(ordreChrono).stream()
                .collect(Collectors.toMap(TerritoryBattle::getId, tb -> tb));
        List<TerritoryBattle> tbHistorique = ordreChrono.stream()
                .map(tbParId::get)
                .collect(Collectors.toList());

        long nbReussis = 0, nbEchecs = 0, nbNonTentes = 0;
        List<JoueurHistoriqueMission> enEchec = new ArrayList<>();
        List<JoueurHistoriqueMission> nonTentes = new ArrayList<>();

        for (Map.Entry<String, String> joueur : nomJoueurs.entrySet()) {
            String playerId = joueur.getKey();
            String playerName = joueur.getValue();

            TbMissionJoueurStatsProjection stats = statsParJoueur.get(playerId);
            long tentes = stats != null && stats.getTentes() != null ? stats.getTentes() : 0;
            long reussis = stats != null && stats.getReussis() != null ? stats.getReussis() : 0;

            StatutMission statut;
            if (reussis > 0) {
                statut = StatutMission.REUSSI;
                nbReussis++;
            } else if (tentes > 0) {
                statut = StatutMission.ECHOUE;
                nbEchecs++;
            } else {
                statut = StatutMission.NON_TENTE;
                nbNonTentes++;
            }

            if (statut != StatutMission.REUSSI) {
                Map<Long, TbMissionHistoriqueProjection> lignesParTbId = historiqueParJoueur
                        .getOrDefault(playerId, List.of())
                        .stream()
                        .collect(Collectors.toMap(TbMissionHistoriqueProjection::getTerritoryBattleId, l -> l, (a, b) -> a));

                List<StatutMission> historique = new ArrayList<>();
                for (Long idPrecedent : ordreChrono) {
                    TbMissionHistoriqueProjection ligne = lignesParTbId.get(idPrecedent);
                    if (ligne == null) {
                        historique.add(StatutMission.NON_TENTE);
                        continue;
                    }
                    long t = ligne.getTentes() != null ? ligne.getTentes() : 0;
                    long r = ligne.getReussis() != null ? ligne.getReussis() : 0;
                    historique.add(r > 0 ? StatutMission.REUSSI : (t > 0 ? StatutMission.ECHOUE : StatutMission.NON_TENTE));
                }

                JoueurHistoriqueMission jhm = new JoueurHistoriqueMission(playerId, playerName, statut, historique);
                if (statut == StatutMission.ECHOUE) enEchec.add(jhm);
                else nonTentes.add(jhm);
            }
        }

        enEchec.sort(Comparator.comparing(JoueurHistoriqueMission::playerName, String.CASE_INSENSITIVE_ORDER));
        nonTentes.sort(Comparator.comparing(JoueurHistoriqueMission::playerName, String.CASE_INSENSITIVE_ORDER));

        return new MissionSpecialeAnalyse(nbReussis, nbEchecs, nbNonTentes, tbHistorique, enEchec, nonTentes);
        }
}