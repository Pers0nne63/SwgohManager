package swgohManager.service;

public sealed interface PlayerIdentifier permits PlayerIdentifier.ByPlayerId, PlayerIdentifier.ByAllyCode {

    record ByPlayerId(String playerId) implements PlayerIdentifier {}
    record ByAllyCode(String allyCode) implements PlayerIdentifier {}

    static PlayerIdentifier of(String playerId, String allyCode) {
        boolean aPlayerId = playerId != null && !playerId.isBlank();
        boolean aAllyCode = allyCode != null && !allyCode.isBlank();

        if (aPlayerId == aAllyCode) {
            throw new IllegalArgumentException("Fournir soit playerId, soit allyCode (un seul des deux, pas les deux ni aucun)");
        }
        return aPlayerId ? new ByPlayerId(playerId) : new ByAllyCode(allyCode);
    }
}