package org.example.filemonitoringapi.subscription;

import org.example.filemonitoringapi.exception.FileWatcherRegistrationException;
import org.example.filemonitoringapi.exception.SubscriptionNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
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
        assertEquals(2, capturedSubscriptions.size());


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
    public void testGetAllSubscriptions() {
        // Przygotowanie danych
        Subscription subscription1 = new Subscription();
        subscription1.setFilePath("testFilePath1");
        subscription1.setEmail("test1@example.com");
        subscription1.setJobId(UUID.randomUUID().toString());
        subscription1.setActive(true);

        Subscription subscription2 = new Subscription();
        subscription2.setFilePath("testFilePath2");
        subscription2.setEmail("test2@example.com");
        subscription2.setJobId(UUID.randomUUID().toString());
        subscription2.setActive(true);

        List<Subscription> subscriptions = Arrays.asList(subscription1, subscription2);
        Page<Subscription> subscriptionPage = new PageImpl<>(subscriptions);

        Pageable pageable = PageRequest.of(0, 10);
        when(subscriptionRepository.findAll(pageable)).thenReturn(subscriptionPage);

        SubscriptionDto dto1 = SubscriptionDto.builder()
                .filePath(subscription1.getFilePath())
                .email(subscription1.getEmail())
                .jobId(subscription1.getJobId())
                .active(subscription1.isActive())
                .build();

        SubscriptionDto dto2 = SubscriptionDto.builder()
                .filePath(subscription2.getFilePath())
                .email(subscription2.getEmail())
                .jobId(subscription2.getJobId())
                .active(subscription2.isActive())
                .build();

        when(subscriptionMapper.toDTO(subscription1)).thenReturn(dto1);
        when(subscriptionMapper.toDTO(subscription2)).thenReturn(dto2);

        // Wywołanie testowanej metody
        Page<SubscriptionDto> result = subscriptionService.getAllSubscriptions(pageable);

        // Weryfikacja
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(subscriptionRepository, times(1)).findAll(pageable);
        verify(subscriptionMapper, times(1)).toDTO(subscription1);
        verify(subscriptionMapper, times(1)).toDTO(subscription2);

        // Sprawdzenie danych w wynikach
        SubscriptionDto resultDto1 = result.getContent().get(0);
        assertEquals(dto1.getEmail(), resultDto1.getEmail());
        assertEquals(dto1.getFilePath(), resultDto1.getFilePath());

        SubscriptionDto resultDto2 = result.getContent().get(1);
        assertEquals(dto2.getEmail(), resultDto2.getEmail());
        assertEquals(dto2.getFilePath(), resultDto2.getFilePath());
    }

    @Test
    public void testGetSubscriptionStatusByJobId_AktywnaSubskrypcja() {
        String jobId = UUID.randomUUID().toString();
        Subscription subscription = new Subscription();
        subscription.setJobId(jobId);
        subscription.setActive(true);

        when(subscriptionRepository.findByJobId(jobId)).thenReturn(Optional.of(subscription));

        boolean status = subscriptionService.getSubscriptionStatusByJobId(jobId);

        assertTrue("Status powinien być prawdziwy dla aktywnej subskrypcji", status);
        verify(subscriptionRepository, times(1)).findByJobId(jobId);
    }

    @Test
    public void testGetSubscriptionStatusByJobId_NieistniejącaSubskrypcja() {
        String jobId = UUID.randomUUID().toString();

        when(subscriptionRepository.findByJobId(jobId)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> subscriptionService.getSubscriptionStatusByJobId(jobId),
                "Oczekiwano zgłoszenia SubscriptionNotFoundException dla nieistniejącej subskrypcji");

        verify(subscriptionRepository, times(1)).findByJobId(jobId);
    }


    @Test
    public void testGetSubscriptionStatusByJobId_NonExistingSubscription() {
        String jobId = UUID.randomUUID().toString();

        when(subscriptionRepository.findByJobId(jobId)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> subscriptionService.getSubscriptionStatusByJobId(jobId),
                "Expected SubscriptionNotFoundException to be thrown for non-existing subscription");

        verify(subscriptionRepository, times(1)).findByJobId(jobId);
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


