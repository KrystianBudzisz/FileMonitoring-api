package org.example.filemonitoringapi.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(String jobId) {
        super("Nie znaleziono subskrypcji z jobId: " + jobId);
    }
}
