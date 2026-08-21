package swgohManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwgohApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwgohApplication.class, args);
        
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);

        System.out.println("================ MEMOIRE JVM ================");
        System.out.println("RAM Max autorisée (-Xmx) : " + maxMemory + " Mo");
        System.out.println("RAM actuellement réservée  : " + totalMemory + " Mo");
        System.out.println("RAM libre dans le pool    : " + freeMemory + " Mo");
        System.out.println("=============================================");
    }
}