package swgohManager.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.service.TbImportStatService;

@RestController
@RequestMapping("/api/tb/import")
@RequiredArgsConstructor
@Slf4j
public class TbImportController {

    private final TbImportStatService tbImportStatService;

    /**
     * Déclenche l'importation d'un fichier JSON de statistiques TB.
     *
     * Exemple cURL :
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
}