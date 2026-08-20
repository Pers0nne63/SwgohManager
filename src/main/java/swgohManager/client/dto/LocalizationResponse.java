package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalizationResponse(
        @JsonProperty("Loc_FRE_FR.txt") String locFreFr
) {}