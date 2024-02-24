package org.example.filemonitoringapi.email;

import lombok.AllArgsConstructor;
import org.example.filemonitoringapi.exception.EmailSendingException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class EmailService {

    private JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String content)  {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (MailException e) {
            throw new EmailSendingException("Nie udało się wysłać emaila do: " + to, e);
        }
    }

}

