package swgohManager.controller;

import swgohManager.service.DockerComlinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/docker")
@RequiredArgsConstructor
public class DockerController {

    private final DockerComlinkService dockerComlinkService;

    @PostMapping("/comlink/update")
    public ResponseEntity<String> updateComlink() {
        String rapport = dockerComlinkService.updateComlink();
        return ResponseEntity.ok(rapport);
    }

    @ExceptionHandler(DockerComlinkService.DockerCommandException.class)
    public ResponseEntity<String> handleDockerError(DockerComlinkService.DockerCommandException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}