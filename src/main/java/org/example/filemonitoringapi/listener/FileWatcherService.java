package org.example.filemonitoringapi.listener;

import lombok.AllArgsConstructor;
import org.example.filemonitoringapi.email.EmailService;
import org.example.filemonitoringapi.exception.FileReadException;
import org.example.filemonitoringapi.exception.FileWatcherRegistrationException;
import org.example.filemonitoringapi.file.FileChange;
import org.example.filemonitoringapi.file.FileChangeRepository;
import org.example.filemonitoringapi.subscription.SubscriptionRepository;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class FileWatcherService {
    private final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, WatchService> watchServices = new ConcurrentHashMap<>();
    private EmailService emailService;
    private SubscriptionRepository subscriptionRepository;
    private FileChangeRepository fileChangeRepository;


    public void registerFileWatcher(Subscription subscription) throws FileWatcherRegistrationException {
        String filePath = subscription.getFilePath();
        Path path = Paths.get(filePath).getParent();
        if (!watchServices.containsKey(filePath)) {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                watchServices.put(filePath, watchService);

                executor.submit(() -> watchFileChanges(filePath, watchService));
                subscriptionRepository.save(subscription);
                String initialContent = readFileContent(filePath);

                FileChange initialChange = new FileChange(filePath, initialContent, LocalDateTime.now(), LocalDateTime.now());
                fileChangeRepository.save(initialChange);

            } catch (IOException e) {
                throw new FileWatcherRegistrationException("Błąd podczas rejestracji FileWatcher dla: " + filePath, e);
            } catch (FileReadException e) {
                throw new FileWatcherRegistrationException("Błąd podczas odczytu pliku: " + filePath, e);
            }
        }
    }


    public void unregisterFileWatcher(Subscription subscription) {
        if (subscription != null) {
            String filePath = subscription.getFilePath();

            WatchService watchService = watchServices.get(filePath);
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                watchServices.remove(filePath);
            }

            subscriptionRepository.delete(subscription);
        }
    }
    @Async
    public void watchFileChanges(String filePath, WatchService watchService) {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Wątek monitorujący zmiany plików został przerwany.");
                return;
            } catch (ClosedWatchServiceException e) {
                logger.info("Serwis WatchService został zamknięty.");
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                try {
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        handleFileModification(filePath);
                    }
                } catch (Exception e) {
                    logger.error("Błąd podczas przetwarzania zmiany pliku: " + filePath, e);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                logger.info("Klucz WatchService stał się nieważny.");
                break;
            }
        }
    }


    private void handleFileModification(String filePath) {
        try {
            String currentContent = readFileContent(filePath);
            Optional<FileChange> lastChange = fileChangeRepository.findTopByFilePathOrderByChangeTimeDesc(filePath);
            String lastKnownChangeContent = lastChange.map(FileChange::getContent).orElse("");

            String newChanges = extractNewChanges(lastKnownChangeContent, currentContent);
            if (!newChanges.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                FileChange change = new FileChange(filePath, newChanges, now, null);
                fileChangeRepository.save(change);
            }
        } catch (FileReadException e) {
            logger.error("Błąd podczas obsługi modyfikacji pliku: " + filePath, e);
        }
    }


    @Scheduled(cron = "0 0 12 * * ?")
    public void sendPendingNotifications() {
        List<FileChange> pendingChanges = fileChangeRepository.findByLastNotificationSentIsNull();
        Map<String, List<FileChange>> changesByFilePath = pendingChanges.stream()
                .collect(Collectors.groupingBy(FileChange::getFilePath));

        changesByFilePath.forEach((filePath, changes) -> {
            List<Subscription> activeSubscriptions = subscriptionRepository.findByFilePathAndActive(filePath, true);

            activeSubscriptions.forEach(subscription -> {
                String changesContent = changes.stream()
                        .map(FileChange::getContent)
                        .collect(Collectors.joining("\n"));
                String emailBody = generateEmailBody(filePath, changesContent);
                emailService.sendEmail(subscription.getEmail(), "Zmiana w pliku: " + filePath, emailBody);

                changes.forEach(change -> {
                    change.setLastNotificationSent(LocalDateTime.now());
                    fileChangeRepository.save(change);
                });

            });
        });
    }


    private String readFileContent(String filePath) throws FileReadException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new FileReadException("Nie udało się odczytać zawartości pliku z: " + filePath, e);
        }
    }

    private String extractNewChanges(String lastKnownChangeContent, String currentContent) {
        List<String> lastLines = Arrays.asList(lastKnownChangeContent.split("\\r?\\n"));
        List<String> currentLines = Arrays.asList(currentContent.split("\\r?\\n"));

        int lastLineIndex = -1;
        if (!lastLines.isEmpty()) {
            String lastLine = lastLines.get(lastLines.size() - 1);
            lastLineIndex = currentLines.lastIndexOf(lastLine);
        }

        List<String> newLines = (lastLineIndex == -1) ? currentLines : currentLines.subList(lastLineIndex + 1, currentLines.size());

        return String.join("\n", newLines);
    }


    private String generateEmailBody(String filePath, String changes) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Dnia " + now.format(formatter) + " zmieniono plik " + filePath + " dopisano do niego:\n" + changes;
    }

}

