package org.example.filemonitoringapi.listener;

import org.example.filemonitoringapi.email.EmailService;
import org.example.filemonitoringapi.exception.FileReadException;
import org.example.filemonitoringapi.exception.FileWatcherRegistrationException;
import org.example.filemonitoringapi.file.FileChange;
import org.example.filemonitoringapi.file.FileChangeRepository;
import org.example.filemonitoringapi.subscription.SubscriptionRepository;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

@Service
public class FileWatcherService {
    private final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Map<String, WatchService> watchServices = new ConcurrentHashMap<>();
    @Autowired
    private EmailService emailService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private FileChangeRepository fileChangeRepository;

    public void registerFileWatcher(Subscription subscription) throws FileWatcherRegistrationException {
        String filePath = subscription.getFilePath();
        Path path = Paths.get(filePath).getParent();

        synchronized (this) {
            if (!watchServices.containsKey(filePath)) {
                try {
                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    watchServices.put(filePath, watchService);

                    executor.submit(() -> watchFileChanges(filePath, watchService));
                    subscriptionRepository.save(subscription);

                    String initialContent = readFileContent(filePath);
                    if (initialContent != null) {
                        fileChangeRepository.save(new FileChange(filePath, initialContent, LocalDateTime.now()));
                        String emailBody = generateInitialEmailBody(filePath, initialContent);
                        emailService.sendEmail(subscription.getEmail(), "Rozpoczęcie monitorowania pliku: " + filePath, emailBody);
                    }
                } catch (IOException e) {
                    // Logowanie błędu
                    logger.error("Błąd podczas rejestracji FileWatcher dla: " + filePath, e);
                    throw new FileWatcherRegistrationException("Błąd podczas rejestracji FileWatcher dla: " + filePath, e);
                } catch (FileReadException e) {
                    // Logowanie błędu
                    logger.error("Błąd podczas odczytu pliku: " + filePath, e);
                    throw new FileWatcherRegistrationException("Błąd podczas odczytu pliku: " + filePath, e);
                }
            }
        }
    }

    public WatchService getWatchService(String filePath) {
        return watchServices.get(filePath);
    }


    public void unregisterFileWatcher(Subscription subscription) {
        if (subscription != null) {
            String filePath = subscription.getFilePath();
            synchronized (this) {
                WatchService watchService = watchServices.get(filePath);
                if (watchService != null) {
                    try {
                        watchService.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    watchServices.remove(filePath);
                }
            }
            subscriptionRepository.delete(subscription);
        }
    }


    private void watchFileChanges(String filePath, WatchService watchService) {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    handleFileModification(filePath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void handleFileModification(String filePath) {
        try {
            String currentContent = readFileContent(filePath);
            if (currentContent != null) {
                Optional<FileChange> lastChange = fileChangeRepository.findTopByFilePathOrderByChangeTimeDesc(filePath);
                String lastKnownChangeContent = lastChange.isPresent() ? lastChange.get().getContent() : "";

                String newChanges = extractNewChanges(lastKnownChangeContent, currentContent);
                if (!newChanges.isEmpty()) {
                    FileChange change = new FileChange(filePath, currentContent, LocalDateTime.now());
                    fileChangeRepository.save(change);

                    sendNotifications(filePath, newChanges);
                }
            }
        } catch (FileReadException e) {
            // Logowanie błędu
            logger.error("Błąd podczas obsługi modyfikacji pliku: " + filePath, e);
        }
    }

    private String readFileContent(String filePath) throws FileReadException {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new FileReadException("Błąd podczas odczytu pliku: " + filePath, e);
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


    private String generateInitialEmailBody(String filePath, String initialContent) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Rozpoczęłaś/eś nasłuchiwanie pliku " + filePath + " dnia " + now.format(formatter) + ". Początkowa zawartość pliku:\n" + initialContent;
    }

    private void sendNotifications(String filePath, String changes) {
        List<Subscription> subscriptions = subscriptionRepository.findByFilePath(filePath);
        for (Subscription sub : subscriptions) {
            if (sub.isActive()) {
                String emailBody = generateEmailBody(filePath, changes);
                emailService.sendEmail(sub.getEmail(), "Zmiana w pliku: " + filePath, emailBody);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void checkForFileChangesAndSendEmails() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            if (subscription.isActive()) {
                String filePath = subscription.getFilePath();
                try {
                    String currentContent = readFileContent(filePath);
                    Optional<FileChange> lastChange = fileChangeRepository.findTopByFilePathOrderByChangeTimeDesc(filePath);
                    String lastKnownChangeContent = lastChange.isPresent() ? lastChange.get().getContent() : "";

                    String newChanges = extractNewChanges(lastKnownChangeContent, currentContent);
                    if (!newChanges.isEmpty()) {
                        FileChange change = new FileChange(filePath, currentContent, LocalDateTime.now());
                        fileChangeRepository.save(change);

                        sendNotifications(filePath, newChanges);
                    }
                } catch (FileReadException e) {
                    // Logowanie błędu
                    logger.error("Błąd podczas sprawdzania zmian w pliku: " + filePath, e);
                }
            }
        }
    }

    private String generateEmailBody(String filePath, String changes) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "Dnia " + now.format(formatter) + " zmieniono plik " + filePath + " dopisano do niego:\n" + changes;
    }

}

