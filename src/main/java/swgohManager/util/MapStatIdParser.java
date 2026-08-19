package swgohManager.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapStatIdParser {

    // Ex: covert_complete_mission_tb3_mixed_phase01_conflict03_bonus_covert01
    private static final Pattern COVERT_MISSION = Pattern.compile(
            "^(covert_complete|covert_round_attempted)_mission_tb3_mixed_phase(\\d{2})_conflict(\\d{2})(_bonus)?_covert(\\d{2})$");

    // Ex: power_zone_tb3_mixed_phase04_conflict03_bonus
    private static final Pattern ZONE = Pattern.compile(
            "^(power|disobey|strike_attempt|strike_encounter|summary|unit_donated)_zone_tb3_mixed_phase(\\d{2})_conflict(\\d{2})(_bonus)?$");

    // Ex: power_round_3
    private static final Pattern ROUND = Pattern.compile(
            "^(power|disobey|strike_attempt|strike_encounter|summary|unit_donated|covert_attempt)_round_(\\d)$");

    public record ParsedMapStat(
            String statType, Integer phase, Integer conflict,
            boolean bonus, Integer covertNum, Integer roundNum
    ) {}

    public static ParsedMapStat parse(String mapStatId) {
        Matcher m = COVERT_MISSION.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(
                    m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                    m.group(4) != null, Integer.parseInt(m.group(5)), null);
        }

        m = ZONE.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(
                    m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                    m.group(4) != null, null, null);
        }

        m = ROUND.matcher(mapStatId);
        if (m.matches()) {
            return new ParsedMapStat(m.group(1), null, null, false, null, Integer.parseInt(m.group(2)));
        }

        // Totaux globaux sans phase/planète (ex: "power", "disobey"...)
        return new ParsedMapStat(mapStatId, null, null, false, null, null);
    }
}