package it.pagopa.qi.jsmmanagement.exceptions;

/**
 * Exception raised when the parsed string is invalid
 */
public class AlertParsingException extends RuntimeException {

    /**
     * Constructor
     *
     * @param alertString the invalid alert parsed
     */
    public AlertParsingException(String alertString) {
        super("Invalid alert format: %s ".formatted(alertString));


    }
}
