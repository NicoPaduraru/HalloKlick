package de.digitalbegleiter;

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
public class DigitalbegleiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalbegleiterApplication.class, args);
    }

    /** Eine Uhr als Bean, damit Tests die Zeit selbst bestimmen können. */
    @Bean
    Clock uhr() {
        return Clock.systemUTC();
    }
}
