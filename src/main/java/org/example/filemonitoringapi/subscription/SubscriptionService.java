package org.example.filemonitoringapi.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.example.filemonitoringapi.listener.FileWatcherService;
import org.example.filemonitoringapi.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.model.Subscription;
import org.example.filemonitoringapi.model.SubscriptionDto;
import org.example.filemonitoringapi.model.SubscriptionMapper;
import org.example.filemonitoringapi.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class SubscriptionService {

    private SubscriptionRepository subscriptionRepository;
    private FileWatcherService fileWatcherService;
    private SubscriptionMapper subscriptionMapper;

    // Tworzenie subskrypcji
    public SubscriptionDto createSubscription(CreateSubscriptionCommand command) throws FileNotFoundException, IOException {
        if (!new File(command.getFilePath()).exists()) {
            throw new FileNotFoundException("Plik " + command.getFilePath() + " nie istnieje.");
        }

        Subscription subscription = subscriptionMapper.fromCreateCommand(command);
        subscription.setJobId(UUID.randomUUID().toString()); // Generowanie unikalnego jobId


        subscriptionRepository.save(subscription);

        fileWatcherService.registerFileWatcher(subscription); // Rejestrowanie watcher'a dla pliku

        return subscriptionMapper.toDTO(subscription);
    }

    //todo usunac optionale sprawdz projekty kiedys zrobic all podobnie!!! Zastosowac exception i przypadki, transactionale i try catche
    // Anulowanie subskrypcji
    public boolean cancelSubscription(String jobId) {
        Optional<Subscription> subscription = subscriptionRepository.findByJobId(jobId);

        if (subscription.isPresent()) {
            subscriptionRepository.delete(subscription.get());
            fileWatcherService.unregisterFileWatcher(subscription.get());
            return true;
        } else {
            return false;
        }
    }

    // Pobieranie subskrypcji na podstawie jobId
    public SubscriptionDto getSubscriptionByJobId(String jobId) {
        Subscription subscription = subscriptionRepository.findByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Nie ma takiej subskrypcji o id: " + jobId));
        return subscriptionMapper.toDTO(subscription);
    }


    @Transactional(readOnly = true)
    public Page<SubscriptionDto> getAllSubscriptions(Pageable pageable) {
        Page<Subscription> subscriptions = subscriptionRepository.findAll(pageable);
        return subscriptions.map(subscriptionMapper::toDTO);
    }


}