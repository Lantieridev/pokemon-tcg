package ar.edu.utn.frc.tup.piii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main class.
 */
@SpringBootApplication
@EnableAsync
public class Application {

    /**
     * Main program.
     * @param args application args
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
