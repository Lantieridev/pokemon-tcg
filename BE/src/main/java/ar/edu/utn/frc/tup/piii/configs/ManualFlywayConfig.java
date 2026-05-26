package ar.edu.utn.frc.tup.piii.configs;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
public class ManualFlywayConfig {

    public ManualFlywayConfig(final DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
