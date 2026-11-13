package de.digitalbegleiter.gemeinsam.sicherheit;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Wer darf was? Die groben Regeln stehen hier. Ob jemand zu einer bestimmten
 * Familie gehört, prüfen zusätzlich die Services.
 */
@Configuration
public class SicherheitsKonfiguration {

    private static final String SENIOR = Angemeldet.SENIOR;
    private static final String ANGEHOERIGER = Angemeldet.ANGEHOERIGER;

    @Bean
    SecurityFilterChain sicherheitsKette(HttpSecurity http, GeraeteAnmelder anmelder,
            SecurityContextRepository kontextSpeicher, ObjectMapper json) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .securityContext(kontext -> kontext.securityContextRepository(kontextSpeicher))
            .sessionManagement(sitzung -> sitzung.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(new GeraeteCookieFilter(anmelder), AuthorizationFilter.class)
            .authorizeHttpRequests(regeln -> regeln
                // Für alle offen
                .requestMatchers(HttpMethod.GET, "/api/anleitungen", "/api/anleitungen/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/einstellungen").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/geraete").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Interne Übersicht: eigener geheimer Schlüssel, wird im Controller geprüft
                .requestMatchers("/api/intern/**").permitAll()
                // Nur Senioren (Geräte)
                .requestMatchers(HttpMethod.POST, "/api/geraete/koppeln").hasRole(SENIOR)
                .requestMatchers(HttpMethod.POST, "/api/hilfeanfragen").hasRole(SENIOR)
                // Nur Angehörige
                .requestMatchers(HttpMethod.DELETE, "/api/geraete/*").hasRole(ANGEHOERIGER)
                .requestMatchers(HttpMethod.PATCH, "/api/hilfeanfragen/*").hasRole(ANGEHOERIGER)
                .requestMatchers("/api/familien", "/api/familien/**").hasRole(ANGEHOERIGER)
                .requestMatchers("/api/push-abos", "/api/push-abos/**").hasRole(ANGEHOERIGER)
                // Alles andere unter /api: angemeldet (Gerät oder Angehöriger)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/actuator/**").denyAll()
                // Die Dateien der Web-App
                .anyRequest().permitAll())
            .exceptionHandling(fehler -> fehler
                .authenticationEntryPoint((anfrage, antwort, e) -> schreibe(json, antwort,
                        HttpStatus.UNAUTHORIZED, "Bitte melden Sie sich an."))
                .accessDeniedHandler((anfrage, antwort, e) -> schreibe(json, antwort,
                        HttpStatus.FORBIDDEN, "Das ist für Sie nicht erlaubt.")))
            .headers(kopf -> kopf
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data: blob:; object-src 'none'; "
                        + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(rechte -> rechte.policy("geolocation=(), microphone=(), payment=()")))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** Angehörige bleiben über eine Sitzung (Cookie) angemeldet. */
    @Bean
    SecurityContextRepository kontextSpeicher() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwortHasher() {
        return new BCryptPasswordEncoder(12);
    }

    private static void schreibe(ObjectMapper json, HttpServletResponse antwort, HttpStatus status, String text)
            throws IOException {
        antwort.setStatus(status.value());
        antwort.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        antwort.setCharacterEncoding("UTF-8");
        json.writeValue(antwort.getWriter(), ProblemDetail.forStatusAndDetail(status, text));
    }
}
