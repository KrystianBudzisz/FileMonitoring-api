package org.example.filemonitoringapi.controller;

import jakarta.validation.Valid;
import org.example.filemonitoringapi.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.model.SubscriptionDto;
import org.example.filemonitoringapi.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    //    @PreAuthorize("hasRole('USER')") // Sprawdza, czy użytkownik ma rolę USER
    @PostMapping("/follow")
    public ResponseEntity<?> followFile(@Valid @RequestBody CreateSubscriptionCommand command) {
        try {
            SubscriptionDto createdSubscription = subscriptionService.createSubscription(command);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Błąd: Plik nie istnieje.");
        } catch (Exception e) {
            // Logowanie błędu
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Wewnętrzny błąd serwera.");
        }
    }

    // Endpoint do anulowania subskrypcji
    @DeleteMapping("/unfollow/{jobId}")
    public ResponseEntity<String> unfollowFile(@PathVariable String jobId) {
        boolean isDeleted = subscriptionService.cancelSubscription(jobId);
        if (isDeleted) {
            return ResponseEntity.ok("Pomyślnie anulowano śledzenie pliku");

        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono subskrypcji z jobId: " + jobId);
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getSubscriptionStatus(@PathVariable String jobId) {
        try {
            SubscriptionDto subscription = subscriptionService.getSubscriptionByJobId(jobId);
            if (subscription != null) {
                return ResponseEntity.ok(subscription);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono subskrypcji z jobId: " + jobId);
            }
        } catch (Exception e) {
            // Logowanie błędu
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Wewnętrzny błąd serwera.");
        }
    }


    @GetMapping
    public ResponseEntity<Page<SubscriptionDto>> getAllSubscriptions(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<SubscriptionDto> subscriptions = subscriptionService.getAllSubscriptions(pageable);
        return ResponseEntity.ok(subscriptions);
    }
}


