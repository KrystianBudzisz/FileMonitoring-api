package org.example.filemonitoringapi.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileChangeRepository extends JpaRepository<FileChange, Long> {
    Optional<FileChange> findTopByFilePathOrderByChangeTimeDesc(String filePath);
    List<FileChange> findByLastNotificationSentIsNull();


}