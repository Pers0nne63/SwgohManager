package swgohManager.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PlayerRequest(Payload payload) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payload(String playerId, String allyCode) {}

    public static PlayerRequest byPlayerId(String playerId) {
        return new PlayerRequest(new Payload(playerId, null));
    }

    public static PlayerRequest byAllyCode(String allyCode) {
        return new PlayerRequest(new Payload(null, allyCode));
    }
}