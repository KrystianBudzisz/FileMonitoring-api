package org.example.filemonitoringapi.subscription;

import jakarta.validation.Valid;
import org.example.filemonitoringapi.subscription.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.subscription.model.SubscriptionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/follow")
    public ResponseEntity<SubscriptionDto> followFile(@Valid @RequestBody CreateSubscriptionCommand command) {
        SubscriptionDto createdSubscription = subscriptionService.createSubscription(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
    }

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

            SubscriptionDto subscription = subscriptionService.getSubscriptionByJobId(jobId);
            if (subscription != null) {
                return ResponseEntity.ok(subscription);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nie znaleziono subskrypcji z jobId: " + jobId);
            }

    }


    @GetMapping
    public ResponseEntity<Page<SubscriptionDto>> getAllSubscriptions(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<SubscriptionDto> subscriptions = subscriptionService.getAllSubscriptions(pageable);
        return ResponseEntity.ok(subscriptions);
    }
}


