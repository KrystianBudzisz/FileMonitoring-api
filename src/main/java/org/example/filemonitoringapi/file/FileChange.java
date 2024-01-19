package org.example.filemonitoringapi.file;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class FileChange {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String filePath;
    private String content;
    private LocalDateTime changeTime;

    public FileChange(String filePath, String content, LocalDateTime changeTime) {
        this.filePath = filePath;
        this.content = content;
        this.changeTime = changeTime;
    }
}
