package swgohManager.controller;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.GuionbotClient;
import swgohManager.service.TbAutoImportService;
import swgohManager.service.TbEventImportService;
import swgohManager.service.TbImportStatService;

@RestController
@RequestMapping("/api/tb/import")
@RequiredArgsConstructor
@Slf4j
public class TbImportController {

    private static final DateTimeFormatter FORMAT_TROU =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private final TbImportStatService tbImportStatService;
    private final TbAutoImportService tbAutoImportService;
    private final TbEventImportService tbEventImportService;
    private final GuionbotClient guionbotClient;
    private final ObjectMapper objectMapper;

    @Value("${app.import.directory:./imports}")
    private String importDirectory;

    /**
     * Import manuel d'un fichier JSON (inchangé).
     * curl -X POST "http://localhost:8080/api/tb/import/round?nomFichier=TBmapstats_round5.json&planId=1&territoryBattleId=10"
     */
    @PostMapping("/round")
    public ResponseEntity<String> importerRound(
            @RequestParam String nomFichier,
            @RequestParam Long planId,
            @RequestParam Long territoryBattleId) {

        log.info("Demande d'importation reçue pour fichier : {}, planId : {}, territoryBattleId : {}",
                nomFichier, planId, territoryBattleId);

        try {
            tbImportStatService.importerStatsRound(nomFichier, planId, territoryBattleId);
            return ResponseEntity.ok("Importation réussie pour le fichier : " + nomFichier);
        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide ou fichier introuvable : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        } catch (IOException e) {
            log.error("Erreur de lecture du fichier JSON : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lecture fichier : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'importation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur système : " + e.getMessage());
        }
    }

    /**
     * Test : récupère le mapstats via l'API, l'écrit dans le dossier d'imports (pour comparaison)
     * puis lance l'import existant.
     * curl -X POST "http://localhost:8080/api/tb/import/round/api?roundNum=1&planId=1&territoryBattleId=10"
     */
    @PostMapping("/round/api")
    public ResponseEntity<String> importerDepuisApi(
            @RequestParam int roundNum,
            @RequestParam Long planId,
            @RequestParam Long territoryBattleId) {

        try {
            JsonNode data = guionbotClient.fetchMapStats();
            String nomFichier = "TBmapstats_round" + roundNum + ".json";
            objectMapper.writeValue(new File(importDirectory, nomFichier), data);

            tbImportStatService.importerStatsRound(nomFichier, planId, territoryBattleId);
            return ResponseEntity.ok("Import API réussi : " + nomFichier);
        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        } catch (IOException e) {
            log.error("Erreur fichier JSON : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur fichier : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de l'import via l'API guionbot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur système : " + e.getMessage());
        }
    }

    /**
     * Déclenche à la main exactement ce que fera le scheduler (guild -> TB/round -> mapstats -> import).
     * curl -X POST "http://localhost:8080/api/tb/import/auto"
     */
    @PostMapping("/auto")
    public ResponseEntity<String> synchroniserAuto() {
        try {
            return ResponseEntity.ok(tbAutoImportService.synchroniser());
        } catch (Exception e) {
            log.error("Erreur lors de la synchro automatique TB", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur : " + e.getMessage());
        }
    }

    /**
     * Import manuel de tb-events via l'API guionbot : 1 appel (donc déconnexion du jeu),
     * sans test de fenêtre de TB. Les événements déjà en base sont ignorés.
     * curl -X POST "http://localhost:8080/api/tb/import/events"
     */
    @PostMapping("/events")
    public ResponseEntity<String> importerEvents() {
        try {
            return ResponseEntity.ok(formaterResultatEvents(tbEventImportService.importerDepuisApi()));
        } catch (Exception e) {
            log.error("Erreur lors de l'import tb-events via l'API guionbot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur : " + e.getMessage());
        }
    }

    /**
     * Rejoue un fichier tb-events déjà téléchargé (sans appel guionbot), lu dans le dossier d'imports.
     * Utile pour tester le parsing et la détection de trous : importer les fichiers dans l'ordre chronologique.
     * curl -X POST "http://localhost:8080/api/tb/import/events/fichier?nomFichier=tb-events_09h44.json"
     */
    @PostMapping("/events/fichier")
    public ResponseEntity<String> importerEventsFichier(@RequestParam String nomFichier) {
        try {
            File fichier = new File(importDirectory, new File(nomFichier).getName());
            if (!fichier.isFile()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Erreur : fichier introuvable : " + fichier.getPath());
            }
            JsonNode racine = objectMapper.readTree(fichier);
            return ResponseEntity.ok(formaterResultatEvents(tbEventImportService.importer(racine.path("data"))));
        } catch (IOException e) {
            log.error("Erreur de lecture du fichier tb-events : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lecture fichier : " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de l'import du fichier tb-events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur : " + e.getMessage());
        }
    }

    private String formaterResultatEvents(TbEventImportService.Resultat r) {
        StringBuilder sb = new StringBuilder("Import tb-events OK : ")
                .append(r.lus()).append(" lus, ")
                .append(r.nouveaux()).append(" nouveaux, ")
                .append(r.dejaPresents()).append(" déjà en base, ")
                .append(r.ignores()).append(" ignorés, ")
                .append(r.trous().size()).append(" trou(s)");
        for (TbEventImportService.Trou t : r.trous()) {
            sb.append("\n - ").append(t.zoneId())
              .append(" : ").append(FORMAT_TROU.format(t.debut()))
              .append(" -> ").append(FORMAT_TROU.format(t.fin()))
              .append(" (").append(t.pointsManquants()).append(" points manquants)");
        }
        return sb.toString();
    }
}