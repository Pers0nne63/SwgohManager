package swgohManager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import swgohManager.service.GacRosterSyncService;
import swgohManager.service.LeaderboardModMoyService;

@RestController
@RequestMapping("/api/gac")
@RequiredArgsConstructor
public class GacController {

    private final GacRosterSyncService gacRosterSyncService;
    private final LeaderboardModMoyService leaderboardModMoyService;

    // 1. Commande existante 1 (inchangée)
    @PostMapping("/sync-top-players")
    public ResponseEntity<GacRosterSyncService.GacSyncResult> syncTopPlayers() {
        return ResponseEntity.ok(gacRosterSyncService.synchroniserTopPlayers());
    }
    
    // 2. Commande existante 2 (inchangée)
    @PostMapping("/calculer-mod-moyen")
    public ResponseEntity<String> calculerModMoyen() {
        return ResponseEntity.ok(leaderboardModMoyService.calculerMoyennes());
    }

    // 3. NOUVELLE COMMANDE COMBINÉE
    @PostMapping("/sync-full")
    public ResponseEntity<GacFullSyncResponse> syncComplet() {
        // Étape 1 : Synchronisation des joueurs du top
        GacRosterSyncService.GacSyncResult syncResult = gacRosterSyncService.synchroniserTopPlayers();
        
        // Étape 2 : Calcul des mods moyens
        String calculResult = leaderboardModMoyService.calculerMoyennes();
        
        // On retourne la réponse combinée
        return ResponseEntity.ok(new GacFullSyncResponse(syncResult, calculResult));
    }

    // Record DTO pour structurer la réponse de la 3e commande
    public record GacFullSyncResponse(
        GacRosterSyncService.GacSyncResult syncResult,
        String modMoyenResult
    ) {}
}