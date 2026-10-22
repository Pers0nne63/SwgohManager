package swgohManager.controller.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Sert les portraits d'unités (/assets/portrait/{bundle}).
 * Premier appel : téléchargement depuis le conteneur ae2 puis enregistrement sur disque.
 * Appels suivants : lecture du fichier local, avec un cache navigateur long.
 */
@Slf4j
@RestController
public class GameAssetController {

    /** Nom de bundle autorisé (évite toute traversée de chemin). */
    private static final Pattern BUNDLE = Pattern.compile("[A-Za-z0-9_]{1,100}");
    private static final Duration DUREE_CACHE_VERSION = Duration.ofHours(6);

    private final WebClient ae2;
    private final WebClient comlink;
    private final Path dossierPortraits;
    private final int versionParDefaut;

    private final ConcurrentHashMap<String, Object> verrous = new ConcurrentHashMap<>();
    private int versionEnCache = 0;
    private Instant versionValideJusque = Instant.EPOCH;

    public GameAssetController(
            @Value("${assets.ae2-url}") String ae2Url,
            @Value("${assets.comlink-url}") String comlinkUrl,
            @Value("${assets.storage-dir}") String storageDir,
            @Value("${assets.fallback-version}") int versionParDefaut) {
        this.ae2 = WebClient.builder()
                .baseUrl(ae2Url)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.comlink = WebClient.builder().baseUrl(comlinkUrl).build();
        this.dossierPortraits = Paths.get(storageDir, "portraits");
        this.versionParDefaut = versionParDefaut;
    }

    @GetMapping("/assets/portrait/{bundle}")
    public ResponseEntity<byte[]> portrait(@PathVariable String bundle) {
        if (!BUNDLE.matcher(bundle).matches()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path fichier = dossierPortraits.resolve(bundle + ".png");
            if (!Files.exists(fichier)) {
                synchronized (verrous.computeIfAbsent(bundle, k -> new Object())) {
                    if (!Files.exists(fichier)) {
                        telecharger(bundle, fichier);
                    }
                }
            }
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .contentType(MediaType.IMAGE_PNG)
                    .body(Files.readAllBytes(fichier));
        } catch (Exception e) {
            log.warn("Portrait indisponible pour le bundle '{}' : {}", bundle, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private void telecharger(String bundle, Path cible) throws IOException {
        int version = versionAssets();
        byte[] png = ae2.get()
                .uri(u -> u.path("/Asset/single")
                        .queryParam("assetName", bundle)
                        .queryParam("version", version)
                        .queryParam("forceReDownload", false)
                        .queryParam("assetOS", 0)
                        .build())
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(90));

        if (png == null || png.length == 0) {
            throw new IOException("Réponse vide d'ae2");
        }
        Files.createDirectories(dossierPortraits);
        // Écriture dans un fichier temporaire puis renommage, pour ne jamais servir une image tronquée
        Path tmp = Files.createTempFile(dossierPortraits, bundle, ".tmp");
        Files.write(tmp, png);
        Files.move(tmp, cible, StandardCopyOption.REPLACE_EXISTING);
        log.info("Portrait téléchargé : {} ({} octets, version assets {})", bundle, png.length, version);
    }

    /** Version des assets lue dans Comlink (/metadata), gardée 6 h ; valeur de repli en configuration. */
    private synchronized int versionAssets() {
        if (versionEnCache != 0 && Instant.now().isBefore(versionValideJusque)) {
            return versionEnCache;
        }
        try {
            JsonNode meta = comlink.post()
                    .uri("/metadata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(20));
            int v = meta != null ? meta.path("assetVersion").asInt(0) : 0;
            if (v > 0) {
                versionEnCache = v;
                versionValideJusque = Instant.now().plus(DUREE_CACHE_VERSION);
                return v;
            }
        } catch (Exception e) {
            log.warn("Version des assets illisible via Comlink : {}", e.getMessage());
        }
        return versionEnCache != 0 ? versionEnCache : versionParDefaut;
    }
}