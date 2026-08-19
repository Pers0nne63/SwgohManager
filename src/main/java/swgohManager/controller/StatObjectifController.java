package swgohManager.controller;

import swgohManager.service.RosterUnitStatObjectifService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatObjectifController {

    private final RosterUnitStatObjectifService rosterUnitStatObjectifService;

    @PostMapping("/objectif")
    public ResponseEntity<String> calculerObjectif() {
        return ResponseEntity.ok(rosterUnitStatObjectifService.calculerPourTousLesJoueurs());
    }
}