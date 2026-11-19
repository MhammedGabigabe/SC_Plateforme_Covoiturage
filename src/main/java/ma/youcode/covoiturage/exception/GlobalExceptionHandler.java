package ma.youcode.covoiturage.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("URL inexistante : {}", request.getRequestURI());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Ressource introuvable",
                "L'URL demandée n'existe pas", request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Action interdite : {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Action interdite", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Règle métier refusée : {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, "Règle métier violée", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Erreur de validation : {}", message);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Erreur de validation", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Corps de requête illisible : {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Le corps de la requête est invalide ou mal formaté", request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String message = "L'en-tête " + ex.getHeaderName() + " est obligatoire";
        log.warn(message);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Requête invalide", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Le paramètre '" + ex.getName() + "' a une valeur invalide : " + ex.getValue();
        log.warn(message);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Requête invalide", message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "Le paramètre '" + ex.getParameterName() + "' est obligatoire";
        log.warn(message);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Requête invalide", message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = "La méthode " + ex.getMethod() + " n'est pas autorisée sur cette URL";
        log.warn(message);
        return buildProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, "Méthode non autorisée", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request) {
        log.error("Erreur interne sur {}", request.getRequestURI(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur interne est survenue", request);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String titre, String message,
                                             HttpServletRequest request) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(status, message);
        probleme.setTitle(titre);
        probleme.setInstance(URI.create(request.getRequestURI()));
        probleme.setProperty("timestamp", LocalDateTime.now());
        return probleme;
    }
}
