package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SkillRaw(String id, List<TierRaw> tier, Boolean isZeta) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TierRaw(Boolean isZetaTier, Boolean isOmicronTier) {}
}