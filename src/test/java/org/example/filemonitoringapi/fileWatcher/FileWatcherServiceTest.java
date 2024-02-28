package org.example.filemonitoringapi.fileWatcher;


import org.example.filemonitoringapi.email.EmailDetails;
import org.example.filemonitoringapi.file.FileChange;
import org.example.filemonitoringapi.file.FileChangeRepository;
import org.example.filemonitoringapi.sender.EmailSender;
import org.example.filemonitoringapi.subscription.SubscriptionRepository;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.example.filemonitoringapi.watcher.FileWatcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class FileWatcherServiceTest {

    @Autowired
    private FileWatcherService fileWatcherService;

    @Autowired
    private FileChangeRepository fileChangeRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private EmailSender emailSender;

    @Captor
    private ArgumentCaptor<EmailDetails> emailDetailsCaptor;

    @Test
    public void shouldSendEmailWhenFileIsModified() throws Exception {
        String testFilePath = "testFile.txt";
        String testEmail = "test@example.com";
        Path tempFile = Files.createTempFile(testFilePath, ".txt");
        Files.writeString(tempFile, "Initial content");

        Subscription subscription = Subscription.builder()
                .filePath(tempFile.toString())
                .email(testEmail)
                .active(true)
                .build();
        subscriptionRepository.save(subscription);

        FileChange fileChange = new FileChange();
        fileChange.setFilePath(tempFile.toString());
        fileChange.setContent("Some new content");
        fileChange.setChangeTime(LocalDateTime.now());
        fileChangeRepository.save(fileChange);

        fileWatcherService.sendPendingNotifications();

        verify(emailSender, times(1)).sendDetails(emailDetailsCaptor.capture());

        EmailDetails sentEmailDetails = emailDetailsCaptor.getValue();
        assertThat(sentEmailDetails.getTo()).isEqualTo(testEmail);
        assertThat(sentEmailDetails.getContent()).contains("Some new content");


        Files.deleteIfExists(tempFile);
    }

}

