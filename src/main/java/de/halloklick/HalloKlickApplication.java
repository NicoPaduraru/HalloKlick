package de.halloklick;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class HalloKlickApplication {

    public static void main(String[] args) {
        SpringApplication.run(HalloKlickApplication.class, args);
    }

    /** Eine Uhr als Bean, damit Tests die Zeit selbst bestimmen können. */
    @Bean
    Clock uhr() {
        return Clock.systemUTC();
    }
}
