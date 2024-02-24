package org.example.filemonitoringapi.subscription;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.filemonitoringapi.subscription.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.subscription.model.SubscriptionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@AllArgsConstructor
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private SubscriptionService subscriptionService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto followFile(@Valid @RequestBody CreateSubscriptionCommand command) {
        return subscriptionService.createSubscription(command);
    }

    @DeleteMapping("{jobId}/unfollow")
    @ResponseStatus(HttpStatus.OK)
    public void unfollowFile(@PathVariable String jobId) {
         subscriptionService.cancelSubscription(jobId);
    }

    @GetMapping("{jobId}/status")
    @ResponseStatus(HttpStatus.OK)
    public boolean getSubscriptionStatus(@PathVariable String jobId) {
        return subscriptionService.getSubscriptionStatusByJobId(jobId);
    }
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<SubscriptionDto> getAllSubscriptions(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        return subscriptionService.getAllSubscriptions(pageable);
    }


}


