package org.example.filemonitoringapi.fileWatcher;

import org.example.filemonitoringapi.file.FileChange;
import org.example.filemonitoringapi.file.FileChangeRepository;
import org.example.filemonitoringapi.listener.FileWatcherService;
import org.example.filemonitoringapi.subscription.SubscriptionRepository;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class FileWatcherServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FileChangeRepository fileChangeRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private FileWatcherService fileWatcherService;

    @Captor
    private ArgumentCaptor<FileChange> fileChangeCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }



    @Test
    public void testSendEmailOnFileChange() throws Exception {
        // Utwórz przykładową subskrypcję
        Subscription subscription = new Subscription();
        subscription.setFilePath("testFilePath");
        subscription.setEmail("test@example.com");
        subscription.setActive(true);

        // Symuluj pobranie subskrypcji na podstawie jobId
        when(subscriptionRepository.findByJobId(anyString())).thenReturn(Optional.of(subscription));

        // Symuluj pobranie zmian w pliku
        FileChange fileChange = new FileChange("testFilePath", "New content", LocalDateTime.now(), null);
        List<FileChange> fileChanges = Collections.singletonList(fileChange);
        when(fileChangeRepository.findByLastNotificationSentIsNull()).thenReturn(fileChanges);

        // Wywołaj metodę, która powinna wysłać email
        fileWatcherService.sendPendingNotifications();

        // Zweryfikuj, czy metoda do wysyłania emaila została wywołana
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        // Zweryfikuj, czy zmiana w pliku została oznaczona jako wysłana
        verify(fileChangeRepository, times(1)).save(fileChangeCaptor.capture());
        FileChange capturedFileChange = fileChangeCaptor.getValue();
        assertNotNull(capturedFileChange.getLastNotificationSent());
    }
}
