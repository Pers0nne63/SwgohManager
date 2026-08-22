package swgohManager.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SyncProgressService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String syncType) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(syncType, emitter);
        emitter.onCompletion(() -> emitters.remove(syncType));
        emitter.onTimeout(() -> emitters.remove(syncType));
        emitter.onError(e -> emitters.remove(syncType));

        // Force l'ouverture immédiate de la connexion côté navigateur (déclenche onopen)
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(syncType);
        }

        return emitter;
    }

    public void notifyProgress(String syncType, int progressPercent, String stepName, String logMessage) {
        SseEmitter emitter = emitters.get(syncType);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of(
                    "percent", progressPercent,
                    "step", stepName,
                    "message", logMessage != null ? logMessage : "",
                    "completed", progressPercent >= 100
                );
                emitter.send(SseEmitter.event().name("progress").data(data));
                if (progressPercent >= 100) {
                    emitter.complete();
                    emitters.remove(syncType);
                }
            } catch (IOException e) {
                emitters.remove(syncType);
            }
        }
    }

    public void notifyError(String syncType, String errorMessage) {
        SseEmitter emitter = emitters.get(syncType);
        if (emitter != null) {
            try {
                Map<String, Object> data = Map.of(
                    "percent", 0,
                    "step", "Erreur",
                    "message", errorMessage,
                    "error", true
                );
                emitter.send(SseEmitter.event().name("progress").data(data));
                emitter.complete();
            } catch (IOException ignored) {
            } finally {
                emitters.remove(syncType);
            }
        }
    }
}