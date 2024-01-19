package org.example.filemonitoringapi.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileChangeRepository extends JpaRepository<FileChange, Long> {
    List<FileChange> findByFilePathOrderByChangeTimeDesc(String filePath);
}