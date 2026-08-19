package swgohManager.controller;

import swgohManager.controller.dto.PlayerSyncRequest;
import swgohManager.service.PlayerIdentifier;
import swgohManager.service.PlayerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerSyncService playerSyncService;

    @PostMapping("/sync")
    public ResponseEntity<PlayerSyncService.PlayerSyncResult> sync(@RequestBody PlayerSyncRequest request) {
        PlayerIdentifier identifier = PlayerIdentifier.of(request.playerId(), request.allyCode());
        return ResponseEntity.ok(playerSyncService.synchroniserJoueur(identifier));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}