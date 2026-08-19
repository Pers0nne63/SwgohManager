package swgohManager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DockerComlinkService {

    @Value("${swgoh.comlink.image}")
    private String image;

    @Value("${swgoh.comlink.container-name}")
    private String containerName;

    @Value("${swgoh.comlink.app-name}")
    private String appName;

    @Value("${swgoh.comlink.port}")
    private int port;

    private static final int TIMEOUT_SECONDS = 60;

    public String updateComlink() {
        StringBuilder rapport = new StringBuilder();

        rapport.append(executerEtLoguer(
                "docker pull " + image,
                List.of("docker", "pull", image)
        ));

        // stop/rm peuvent échouer si le conteneur n'existe pas encore — on ne bloque pas le flux pour autant
        rapport.append(executerEtLoguer(
                "docker stop " + containerName,
                List.of("docker", "stop", containerName),
                false
        ));

        rapport.append(executerEtLoguer(
                "docker rm " + containerName,
                List.of("docker", "rm", containerName),
                false
        ));

        rapport.append(executerEtLoguer(
                "docker run " + containerName,
                List.of("docker", "run",
                        "--name", containerName,
                        "-d",
                        "--restart", "always",
                        "--env", "APP_NAME=" + appName,
                        "-p", port + ":3000",
                        image)
        ));

        log.info("Mise à jour du comlink terminée");
        return rapport.toString();
    }

    private String executerEtLoguer(String description, List<String> commande) {
        return executerEtLoguer(description, commande, true);
    }

    private String executerEtLoguer(String description, List<String> commande, boolean echecBloquant) {
        log.info("Exécution : {}", description);
        try {
            ProcessBuilder pb = new ProcessBuilder(commande);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String sortie = lireSortie(process);
            boolean termine = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!termine) {
                process.destroyForcibly();
                throw new DockerCommandException("Timeout dépassé pour : " + description);
            }

            int exitCode = process.exitValue();
            log.debug("Sortie [{}] : {}", description, sortie);

            if (exitCode != 0 && echecBloquant) {
                throw new DockerCommandException(
                        "Échec de la commande [" + description + "], code retour " + exitCode + " : " + sortie);
            }

            return description + " -> OK\n";

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String message = "Erreur lors de l'exécution [" + description + "] : " + e.getMessage();
            log.error(message, e);
            if (echecBloquant) {
                throw new DockerCommandException(message, e);
            }
            return description + " -> ignoré (" + e.getMessage() + ")\n";
        }
    }

    private String lireSortie(Process process) throws IOException {
        StringBuilder sortie = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                sortie.append(ligne).append(System.lineSeparator());
            }
        }
        return sortie.toString();
    }

    public static class DockerCommandException extends RuntimeException {
        public DockerCommandException(String message) {
            super(message);
        }

        public DockerCommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}