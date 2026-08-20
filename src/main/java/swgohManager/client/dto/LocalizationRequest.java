package swgohManager.client.dto;

public record LocalizationRequest(Payload payload, boolean unzip) {
    public record Payload(String id) {}

    public static LocalizationRequest of(String id) {
        return new LocalizationRequest(new Payload(id), true);
    }
}