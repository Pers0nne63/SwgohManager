package swgohManager.controller;

import java.io.File;
import java.io.IOException;

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
import swgohManager.service.TbImportStatService;

@RestController
@RequestMapping("/api/tb/import")
@RequiredArgsConstructor
@Slf4j
public class TbImportController {

    private final TbImportStatService tbImportStatService;
    private final TbAutoImportService tbAutoImportService;
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
}