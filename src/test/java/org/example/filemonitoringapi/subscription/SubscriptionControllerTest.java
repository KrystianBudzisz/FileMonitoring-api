package org.example.filemonitoringapi.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.filemonitoringapi.subscription.model.CreateSubscriptionCommand;
import org.example.filemonitoringapi.subscription.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SubscriptionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        subscriptionRepository.deleteAll();
    }

    @Test
    @WithMockUser
    public void testFollowFile() throws Exception {
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setFilePath("src/main/resources/zakupy.txt");
        command.setEmail("test@example.com");

        mockMvc.perform(post("/api/subscriptions/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.filePath").value("src/main/resources/zakupy.txt"))
                .andExpect(jsonPath("$.active").value(true));

        // Sprawdzenie, czy dane zostały zapisane w bazie danych
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        assertFalse(subscriptions.isEmpty());
        assertEquals("test@example.com", subscriptions.get(0).getEmail());
        assertEquals("src/main/resources/zakupy.txt", subscriptions.get(0).getFilePath());
        assertTrue(subscriptions.get(0).isActive());
    }



    @Test
    public void testFollowFile_FileNotFound() throws Exception {
        // Przygotowanie danych testowych
        CreateSubscriptionCommand command = new CreateSubscriptionCommand();
        command.setFilePath("/nonexistent/file.txt");
        command.setEmail("test@example.com");

        // Wywołanie kontrolera
        mockMvc.perform(post("/api/subscriptions/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().is(500))
                .andExpect(content().string("Wewnętrzny błąd serwera."));
    }

    @Test
    public void testUnfollowFile() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setFilePath("src/main/resources/zakupy.txt");
        subscription.setEmail("test@example.com");
        subscription.setActive(true);
        subscription.setJobId(UUID.randomUUID().toString());

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        mockMvc.perform(delete("/api/subscriptions/{jobId}/unfollow", savedSubscription.getJobId()))
                .andExpect(status().isOk());

        Optional<Subscription> subscriptions = subscriptionRepository.findByJobId(savedSubscription.getJobId());
        assertFalse(subscriptions.isPresent());
    }

    @Test
    public void testGetSubscriptionStatus() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setFilePath("src/main/resources/zakupy.txt");
        subscription.setEmail("test@example.com");
        subscription.setActive(true);
        subscription.setJobId(UUID.randomUUID().toString());

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        mockMvc.perform(get("/api/subscriptions/{jobId}/status", savedSubscription.getJobId()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testGetAllSubscriptions() throws Exception {
        Subscription subscription1 = new Subscription();
        subscription1.setFilePath("src/main/resources/zakupy.txt");
        subscription1.setEmail("test1@example.com");
        subscription1.setActive(true);
        subscription1.setJobId(UUID.randomUUID().toString());
        subscriptionRepository.save(subscription1);

        Subscription subscription2 = new Subscription();
        subscription2.setFilePath("test/path2");
        subscription2.setEmail("test2@example.com");
        subscription2.setActive(true);
        subscription2.setJobId(UUID.randomUUID().toString());
        subscriptionRepository.save(subscription2);

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11));
    }
}

