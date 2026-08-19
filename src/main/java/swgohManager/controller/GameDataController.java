package swgohManager.controller;

import swgohManager.service.GameDataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamedata")
@RequiredArgsConstructor
public class GameDataController {

    private final GameDataSyncService gameDataSyncService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncAll() {
        return ResponseEntity.ok(gameDataSyncService.synchroniserToutesLesDonnees());
    }
}