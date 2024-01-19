package org.example.filemonitoringapi.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class CreateSubscriptionCommand {
    @NotBlank(message = "Ścieżka pliku nie może być pusta")
    private String filePath;

    @Email(message = "Nieprawidłowy format adresu email")
    @NotBlank(message = "Adres email nie może być pusty")
    private String email;

    private String jobId;

}
