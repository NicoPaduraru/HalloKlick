package de.halloklick.gemeinsam;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
class SprachKonfiguration {

    /**
     * Spring fragt diese Bean bei jeder Anfrage nach der Sprache. Wir nehmen die Sprache,
     * die die Web-App mitschickt, und sonst Deutsch.
     */
    @Bean
    LocaleResolver localeResolver() {
        return new LocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest anfrage) {
                return Sprache.von(anfrage).locale();
            }

            @Override
            public void setLocale(HttpServletRequest anfrage, HttpServletResponse antwort, Locale locale) {
                // Die Sprache kommt immer aus der Anfrage, hier gibt es nichts zu speichern
            }
        };
    }
}
