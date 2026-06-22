package ar.edu.utn.frc.tup.piii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

/**
 * Main class.
 */
@SpringBootApplication
@EnableAsync
public class Application {

    /**
     * Main program.
     * 
     * @param args application args
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }

    /**
     * Bean for RestClient.Builder to resolve RestClientPokemonTcgApiClient
     * dependency injection.
     * 
     * @return a new RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @org.springframework.context.annotation.Profile("dev")
    public org.springframework.boot.ApplicationRunner devDataInjector(
            ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository userRepository) {
        return args -> {
            userRepository.findByUsername("prueba1").ifPresent(user -> {
                user.setXp(20000);
                user.setLevel(25);
                user.setPokecoins(5000);
                userRepository.save(user);
                System.out.println("====== HACK: INYECTADOS 20,000 XP A 'prueba1' LOCALMENTE ======");
            });
        };
    }
}
