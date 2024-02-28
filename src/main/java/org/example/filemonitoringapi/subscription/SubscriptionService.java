package org.example.filemonitoringapi.subscription;

import lombok.AllArgsConstructor;
import org.example.filemonitoringapi.exception.FileWatcherRegistrationException;
import org.example.filemonitoringapi.exception.SubscriptionCreationException;
import org.example.filemonitoringapi.exception.SubscriptionNotFoundException;
import org.example.filemonitoringapi.watcher.FileWatcherService;
import org.example.filemonitoringapi.subscription.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.example.filemonitoringapi.subscription.model.SubscriptionDto;
import org.example.filemonitoringapi.subscription.model.SubscriptionMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@AllArgsConstructor
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final FileWatcherService fileWatcherService;
    private final SubscriptionMapper subscriptionMapper;


    @Transactional
    public SubscriptionDto createSubscription(CreateSubscriptionCommand command) {
        Subscription subscription = subscriptionMapper.fromCreateCommand(command);
        subscription.setJobId(UUID.randomUUID().toString());
        subscription.setActive(true);

        try {
            subscriptionRepository.save(subscription);
            fileWatcherService.registerFileWatcher(subscription);
        } catch (FileWatcherRegistrationException e) {
            throw new SubscriptionCreationException("Nie udało się utworzyć subskrypcji", e);
        }

        return subscriptionMapper.toDTO(subscription);
    }


    @Transactional
    public boolean cancelSubscription(String jobId) {
        Subscription subscription = subscriptionRepository.findByJobId(jobId).orElseThrow(() -> new SubscriptionNotFoundException("Nie ma takiej subskrypcji o id: " + jobId));

        fileWatcherService.unregisterFileWatcher(subscription);
        subscriptionRepository.delete(subscription);
        return true;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriptionByJobId(String jobId) {
        Subscription subscription = subscriptionRepository.findByJobId(jobId).orElseThrow(() -> new SubscriptionNotFoundException("Nie ma takiej subskrypcji o id: " + jobId));
        return subscriptionMapper.toDTO(subscription);
    }

    @Transactional(readOnly = true)
    public boolean getSubscriptionStatusByJobId(String jobId) {
        Subscription subscription = subscriptionRepository.findByJobId(jobId).orElseThrow(() -> new SubscriptionNotFoundException("Nie ma takiej subskrypcji o id: " + jobId));
        return subscription.isActive();
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionDto> getAllSubscriptions(Pageable pageable) {
        Page<Subscription> subscriptions = subscriptionRepository.findAll(pageable);
        return subscriptions.map(subscriptionMapper::toDTO);
    }
}