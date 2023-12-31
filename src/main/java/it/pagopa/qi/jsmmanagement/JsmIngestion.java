package it.pagopa.qi.jsmmanagement;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.pagopa.qi.jsmmanagement.exceptions.AlertParsingException;
import it.pagopa.qi.jsmmanagement.exceptions.JiraTicketCreationException;
import it.pagopa.generated.qi.events.v1.Alert;
import it.pagopa.generated.qi.events.v1.AlertDetails;
import it.pagopa.qi.jsmmanagement.config.JiraRestClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class JsmIngestion {

    private final Logger logger;

    private final ObjectMapper objectMapper;

    private final JiraRestClientConfig jiraRestClientConfig;

    private final JiraRestClient jiraRestClient;


    /**
     * Default constructor
     */
    public JsmIngestion() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(new JavaTimeModule());
        this.jiraRestClientConfig = new JiraRestClientConfig();
        this.jiraRestClient = jiraRestClientConfig.jiraRestClient();
    }

    /**
     * Field injected constructor
     *
     * @param logger               - the logger instance to be used
     * @param objectMapper         - the object mapper instance to be used
     * @param jiraRestClientConfig - the jira rest client configuration class to be used
     * @param jiraRestClient       - the jira rest client to be used
     */
    JsmIngestion(Logger logger, ObjectMapper objectMapper, JiraRestClientConfig jiraRestClientConfig, JiraRestClient jiraRestClient) {
        this.logger = logger;
        this.objectMapper = objectMapper;
        this.jiraRestClientConfig = jiraRestClientConfig;
        this.jiraRestClient = jiraRestClient;
    }

    @FunctionName("EventHubQiJsmProcessor")
    public void processJsmAlert(
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
            IssueRestClient issueClient = jiraRestClient.getIssueClient();
            String projectKey = jiraRestClientConfig.getPpiProjectId();
            long issueTypeId = jiraRestClientConfig.getPpiIssueTypeId();
            String environment = jiraRestClientConfig.getEnvironment();
            AlertDetails alertDetails = alert.getDetails();
            String ticketSummary = "A new KPI %s alert was triggered for %s".formatted(alertDetails.getCode(), alertDetails.getOwner());
            String ticketDescription =
                    """
                            Alert details:
                            KPI code: [%s]
                            Owner: [%s]
                            Threshold: [%s]
                            Value: [%s]
                            Alert triggering date: [%s]
                            """.formatted(alertDetails.getCode(), alertDetails.getOwner(), alertDetails.getThreshold(), alertDetails.getValue(), alertDetails.getTriggerDate());
            IssueInput newIssue = new IssueInputBuilder(
                    projectKey, issueTypeId, "[QI - pagoPA] - [%s] %s".formatted(environment, ticketSummary))
                    .setFieldValue("description", ticketDescription)
                    .build();

            String ticketId = issueClient.createIssue(newIssue).claim().getKey();
            logger.info("Ticket created with ID = {}", ticketId);
        } catch (JsonProcessingException e) {
            logger.error("Error reading value", e);
            throw new AlertParsingException(alertBody);
        } catch (RestClientException e) {
            logger.error("Error opening ticket to JSM", e);
            throw new JiraTicketCreationException(alertBody);
        }
    }
}
