package swgohManager.service;

import swgohManager.model.ExternalPlayerTbScore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalTbStatsService {

    public record TbSynthese(Instant endTime, Long vaguesP3, Long vaguesP4, Long vaguesP5, Long vaguesP6, Long msTenteesTotal) {}

    public record MsStats(
            Instant endTime,
            Integer qiraT, Integer qiraW, Integer jkckT, Integer jkckW,
            Integer sawT, Integer sawW, Integer revaT, Integer revaW,
            Integer bkmT, Integer bkmW, Integer merrinT, Integer merrinW,
            Integer clonesT, Integer clonesW, Integer inquisT, Integer inquisW,
            Integer l337T, Integer l337W, Integer yhanT, Integer yhanW
    ) {}

    // Mêmes constantes que TbScoreJoueurRepository.findPlayerTbMSStats, une seule fois par mission
    private static final Map<String, String> ATTEMPTED_IDS = Map.ofEntries(
            Map.entry("qira", "covert_round_attempted_mission_tb3_mixed_phase01_conflict03_covert01"),
            Map.entry("jkck", "covert_round_attempted_mission_tb3_mixed_phase02_conflict01_covert01"),
            Map.entry("saw", "covert_round_attempted_mission_tb3_mixed_phase03_conflict01_covert01"),
            Map.entry("reva", "covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert01"),
            Map.entry("bkm", "covert_round_attempted_mission_tb3_mixed_phase03_conflict03_covert02"),
            Map.entry("merrin", "covert_round_attempted_mission_tb3_mixed_phase03_conflict02_covert01"),
            Map.entry("clones", "covert_round_attempted_mission_tb3_mixed_phase03_conflict01_bonus_covert01"),
            Map.entry("inquis", "covert_round_attempted_mission_tb3_mixed_phase04_conflict02_covert01"),
            Map.entry("l337", "covert_round_attempted_mission_tb3_mixed_phase04_conflict03_covert01"),
            Map.entry("yhan", "covert_round_attempted_mission_tb3_mixed_phase05_conflict03_covert01")
    );

    private static final Map<String, String> COMPLETED_IDS = Map.ofEntries(
            Map.entry("qira", "covert_complete_mission_tb3_mixed_phase01_conflict03_covert01"),
            Map.entry("jkck", "covert_complete_mission_tb3_mixed_phase02_conflict01_covert01"),
            Map.entry("saw", "covert_complete_mission_tb3_mixed_phase03_conflict01_covert01"),
            Map.entry("reva", "covert_complete_mission_tb3_mixed_phase03_conflict03_covert01"),
            Map.entry("bkm", "covert_complete_mission_tb3_mixed_phase03_conflict03_covert02"),
            Map.entry("merrin", "covert_complete_mission_tb3_mixed_phase03_conflict02_covert01"),
            Map.entry("clones", "covert_complete_mission_tb3_mixed_phase03_conflict01_bonus_covert01"),
            Map.entry("inquis", "covert_complete_mission_tb3_mixed_phase04_conflict02_covert01"),
            Map.entry("l337", "covert_complete_mission_tb3_mixed_phase04_conflict03_covert01"),
            Map.entry("yhan", "covert_complete_mission_tb3_mixed_phase05_conflict03_covert01")
    );

    public TbSynthese calculerSynthese(List<ExternalPlayerTbScore> lignes) {
        if (lignes.isEmpty()) return null;

        Instant endTime = lignes.get(0).getTbEndTime();

        Map<Integer, Long> vaguesParRound = new LinkedHashMap<>();
        long msTenteesTotal = 0;

        for (ExternalPlayerTbScore l : lignes) {
            long score = l.getScore() != null ? l.getScore() : 0L;
            if ("strike_encounter".equals(l.getStatType()) && l.getRoundNum() != null) {
                vaguesParRound.merge(l.getRoundNum(), score, Long::sum);
            }
            if ("covert_attempt".equals(l.getStatType())) {
                msTenteesTotal += score;
            }
        }

        return new TbSynthese(
                endTime,
                vaguesParRound.getOrDefault(3, 0L),
                vaguesParRound.getOrDefault(4, 0L),
                vaguesParRound.getOrDefault(5, 0L),
                vaguesParRound.getOrDefault(6, 0L),
                msTenteesTotal
        );
    }

    public MsStats calculerMsStats(List<ExternalPlayerTbScore> lignes) {
        if (lignes.isEmpty()) return null;

        Instant endTime = lignes.get(0).getTbEndTime();
        Map<String, Long> scoreParMapStatId = new LinkedHashMap<>();
        for (ExternalPlayerTbScore l : lignes) {
            scoreParMapStatId.put(l.getMapStatId(), l.getScore() != null ? l.getScore() : 0L);
        }

        return new MsStats(
                endTime,
                flag("qira", ATTEMPTED_IDS, scoreParMapStatId), flag("qira", COMPLETED_IDS, scoreParMapStatId),
                flag("jkck", ATTEMPTED_IDS, scoreParMapStatId), flag("jkck", COMPLETED_IDS, scoreParMapStatId),
                flag("saw", ATTEMPTED_IDS, scoreParMapStatId), flag("saw", COMPLETED_IDS, scoreParMapStatId),
                flag("reva", ATTEMPTED_IDS, scoreParMapStatId), flag("reva", COMPLETED_IDS, scoreParMapStatId),
                flag("bkm", ATTEMPTED_IDS, scoreParMapStatId), flag("bkm", COMPLETED_IDS, scoreParMapStatId),
                flag("merrin", ATTEMPTED_IDS, scoreParMapStatId), flag("merrin", COMPLETED_IDS, scoreParMapStatId),
                flag("clones", ATTEMPTED_IDS, scoreParMapStatId), flag("clones", COMPLETED_IDS, scoreParMapStatId),
                flag("inquis", ATTEMPTED_IDS, scoreParMapStatId), flag("inquis", COMPLETED_IDS, scoreParMapStatId),
                flag("l337", ATTEMPTED_IDS, scoreParMapStatId), flag("l337", COMPLETED_IDS, scoreParMapStatId),
                flag("yhan", ATTEMPTED_IDS, scoreParMapStatId), flag("yhan", COMPLETED_IDS, scoreParMapStatId)
        );
    }

    private Integer flag(String cle, Map<String, String> idsMap, Map<String, Long> scoreParMapStatId) {
        String mapStatId = idsMap.get(cle);
        Long score = scoreParMapStatId.get(mapStatId);
        return score != null ? score.intValue() : 0;
    }
}