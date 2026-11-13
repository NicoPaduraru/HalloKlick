package de.halloklick.gemeinsam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Alle Fehler kommen einheitlich als ProblemDetail (RFC 9457) zurück. */
@RestControllerAdvice
public class FehlerBehandlung extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FehlerBehandlung.class);

    @ExceptionHandler(ApiFehler.class)
    ProblemDetail apiFehler(ApiFehler fehler) {
        return ProblemDetail.forStatusAndDetail(fehler.status(), fehler.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail keinZugriff(AccessDeniedException fehler) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Das ist für Sie nicht erlaubt.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unerwartet(Exception fehler) {
        // Absichtlich ohne Fehlertext: Er könnte Nachrichteninhalte oder E-Mail-Adressen enthalten.
        LOG.error("Unerwarteter Fehler vom Typ {}", fehler.getClass().getName());
        LOG.debug("Einzelheiten zum unerwarteten Fehler", fehler);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Es ist ein Fehler aufgetreten. Bitte versuchen Sie es später noch einmal.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException fehler,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String text = fehler.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Die Eingabe ist nicht gültig.");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, text);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException fehler,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "Das Foto ist zu groß. Es darf höchstens 5 MB haben.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }
}
