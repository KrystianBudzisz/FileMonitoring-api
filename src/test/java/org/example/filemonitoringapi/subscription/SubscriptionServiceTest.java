package org.example.filemonitoringapi.subscription;

import org.example.filemonitoringapi.exception.FileWatcherRegistrationException;
import org.example.filemonitoringapi.listener.FileWatcherService;
import org.example.filemonitoringapi.subscription.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.example.filemonitoringapi.subscription.model.SubscriptionDto;
import org.example.filemonitoringapi.subscription.model.SubscriptionMapper;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FileWatcherService fileWatcherService;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateSubscription() throws FileWatcherRegistrationException {
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setFilePath("testFilePath");
        command.setEmail("test@example.com");

        Subscription subscription = new Subscription();
        subscription.setJobId(UUID.randomUUID().toString());
        subscription.setActive(true);

        when(subscriptionMapper.fromCreateCommand(command)).thenReturn(subscription);

        subscriptionService.createSubscription(command);

        verify(subscriptionRepository, times(1)).save(subscriptionCaptor.capture());
        verify(fileWatcherService, times(1)).registerFileWatcher(subscriptionCaptor.capture());

        List<Subscription> capturedSubscriptions = subscriptionCaptor.getAllValues();
        assertEquals(2, capturedSubscriptions.size());

    }

    @Test
    public void testCancelSubscription() {
        String jobId = "testJobId";
        Subscription subscription = new Subscription();
        when(subscriptionRepository.findByJobId(jobId)).thenReturn(Optional.of(subscription));

        boolean result = subscriptionService.cancelSubscription(jobId);

        verify(fileWatcherService, times(1)).unregisterFileWatcher(subscriptionCaptor.capture());
        verify(subscriptionRepository, times(1)).delete(subscriptionCaptor.capture());

        List<Subscription> capturedSubscriptions = subscriptionCaptor.getAllValues();
        assertEquals(2, capturedSubscriptions.size()); // Expecting 2 captures (unregisterFileWatcher and delete)


        assertTrue(result);
    }

    @Test
    public void testGetSubscriptionByJobId() {
        String jobId = "testJobId";
        Subscription subscription = new Subscription();
        subscription.setJobId(jobId);

        when(subscriptionRepository.findByJobId(jobId)).thenReturn(Optional.of(subscription));

        SubscriptionDto expectedDto = SubscriptionDto.builder()
                .filePath(subscription.getFilePath())
                .email(subscription.getEmail())
                .jobId(subscription.getJobId())
                .active(subscription.isActive())
                .build();
        when(subscriptionMapper.toDTO(subscription)).thenReturn(expectedDto);

        SubscriptionDto actualDto = subscriptionService.getSubscriptionByJobId(jobId);

        assertNotNull(actualDto, "SubscriptionDto should not be null");
        assertEquals(expectedDto, actualDto, "SubscriptionDto should match the expected DTO");
        verify(subscriptionRepository, times(1)).findByJobId(jobId);
        verify(subscriptionMapper, times(1)).toDTO(subscription);
    }


    @Test
    public void testCreateSubscriptionWithBlankFilePath() {
        CreateSubscriptionCommand command = CreateSubscriptionCommand.builder()
                .filePath("")
                .email("test@example.com")
                .build();

        assertThrows(NullPointerException.class, () -> subscriptionService.createSubscription(command));
    }

    @Test
    public void testCreateSubscriptionWithBlankEmail() {
        CreateSubscriptionCommand command = CreateSubscriptionCommand.builder()
                .filePath("testFilePath")
                .email("")
                .build();

        assertThrows(NullPointerException.class, () -> subscriptionService.createSubscription(command));
    }

    @Test
    public void testCreateSubscriptionWithInvalidEmail() {
        CreateSubscriptionCommand command = CreateSubscriptionCommand.builder()
                .filePath("testFilePath")
                .email("invalid-email")
                .build();

        assertThrows(NullPointerException.class, () -> subscriptionService.createSubscription(command));
    }
}


