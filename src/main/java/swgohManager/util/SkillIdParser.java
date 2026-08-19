package swgohManager.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillIdParser {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^([a-zA-Z]+)skill_(.+)$");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d{2})$");

    // Seuls ces types peuvent légitimement être numérotés (plusieurs compétences du même type sur une unité)
    private static final Set<String> TYPES_NUMEROTABLES = Set.of("special", "unique", "contract", "hardware");

    public record ParsedSkillId(String type, Integer numero) {}

    public static ParsedSkillId parse(String skillId) {
        Matcher m = TYPE_PATTERN.matcher(skillId);
        if (!m.matches()) {
            return new ParsedSkillId(skillId, null);
        }

        String type = m.group(1).toLowerCase();
        String reste = m.group(2);

        if (!TYPES_NUMEROTABLES.contains(type)) {
            return new ParsedSkillId(type, null);
        }

        Matcher numMatcher = TRAILING_NUMBER.matcher(reste);
        Integer numero = numMatcher.find() ? Integer.parseInt(numMatcher.group(1)) : null;

        return new ParsedSkillId(type, numero);
    }
}