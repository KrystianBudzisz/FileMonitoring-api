package org.example.filemonitoringapi.subscription;

import org.example.filemonitoringapi.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByJobId(String jobId);

    List<Subscription> findByFilePathAndActive(String filePath, boolean active);


}

