package swgohManager.service;

import swgohManager.client.SwgohDataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizationService {

    private final SwgohDataClient swgohDataClient;

    private Map<String, String> traductionsEnCache;

    public Map<String, String> getTraductions() {
        if (traductionsEnCache == null) {
            rafraichir();
        }
        return traductionsEnCache;
    }

    public String rafraichir() {
        String version = swgohDataClient.getLatestLocalizationVersion();
        traductionsEnCache = swgohDataClient.getLocalizationFrancaise(version);
        String resultat = String.format("%d traduction(s) chargée(s) (version %s)", traductionsEnCache.size(), version);
        log.info(resultat);
        return resultat;
    }

    public String traduire(String nameKey) {
        if (nameKey == null) return null;
        return getTraductions().getOrDefault(nameKey, nameKey);
    }
}