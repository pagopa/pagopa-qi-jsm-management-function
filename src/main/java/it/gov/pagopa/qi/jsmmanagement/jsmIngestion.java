package it.gov.pagopa.qi.jsmmanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.pagopa.generated.qi.events.v1.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class jsmIngestion {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(new JavaTimeModule());

    @FunctionName("EventHubQiJsmProcessor")
    public void processJsmAlert (
            @EventHubTrigger(
                    name = "QiJsmEvent",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.ONE)
            String alertBody,
            final ExecutionContext context) {
        try {
            logger.info("Received new alert webhook trigger request with invocation id: [{}]", context.getInvocationId());
            Alert alert = objectMapper.readValue(alertBody, Alert.class);
            logger.info("JSM function called at {} with alert {}", LocalDateTime.now(), alert);
        } catch (JsonProcessingException e) {
            logger.error("Error reading value", e);
        }
    }
}
