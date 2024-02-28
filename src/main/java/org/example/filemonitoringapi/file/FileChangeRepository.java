package org.example.filemonitoringapi.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileChangeRepository extends JpaRepository<FileChange, Long> {
    Optional<FileChange> findTopByFilePathOrderByChangeTimeDesc(String filePath);

    List<FileChange> findByLastNotificationSentIsNull();

}