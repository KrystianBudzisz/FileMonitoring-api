package org.example.filemonitoringapi.repository;

import org.example.filemonitoringapi.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByFilePath(String filePath);

    Optional<Subscription> findByJobId(String jobId);
}

