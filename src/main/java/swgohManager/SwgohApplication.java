package swgohManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwgohApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwgohApplication.class, args);
    }
}