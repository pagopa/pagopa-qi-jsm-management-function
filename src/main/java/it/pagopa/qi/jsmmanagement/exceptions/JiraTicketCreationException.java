package it.pagopa.qi.jsmmanagement.exceptions;

public class JiraTicketCreationException extends RuntimeException {

    public JiraTicketCreationException(String alertFingerprint) {
        super("Error opening ticket for alert with body: [%s]".formatted(alertFingerprint));
    }
}
