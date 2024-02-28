package org.example.filemonitoringapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFoundException(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Błąd: Plik nie istnieje.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Brak autoryzacji.");
    }

    @ExceptionHandler(FileReadException.class)
    public ResponseEntity<String> handleFileReadException(FileReadException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd odczytu pliku: " + ex.getMessage());
    }

    @ExceptionHandler(FileWatcherRegistrationException.class)
    public ResponseEntity<String> handleFileWatcherRegistrationException(FileWatcherRegistrationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Błąd rejestracji obserwatora pliku: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Wewnętrzny błąd serwera.");
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<String> handleSubscriptionNotFoundException(SubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono subskrypcji: " + ex.getMessage());
    }
    @ExceptionHandler(SubscriptionCreationException.class)
    public ResponseEntity<String> handleSubscriptionCreationException(SubscriptionCreationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Błąd podczas tworzenia subskrypcji: " + ex.getMessage());
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<String> handleEmailSendingException(EmailSendingException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Nie udało się wysłać e-maila: " + ex.getMessage());
    }

}
