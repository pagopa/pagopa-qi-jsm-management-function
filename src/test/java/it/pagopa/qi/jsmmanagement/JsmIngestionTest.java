package it.pagopa.qi.jsmmanagement;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import io.atlassian.util.concurrent.Promise;
import it.pagopa.generated.qi.events.v1.Alert;
import it.pagopa.generated.qi.events.v1.AlertDetails;
import it.pagopa.qi.jsmmanagement.config.JiraRestClientConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JsmIngestionTest {

    private final ExecutionContext context = Mockito.mock(ExecutionContext.class);

    private final JiraRestClient jiraRestClient = Mockito.mock(JiraRestClient.class);

    private final IssueRestClient issueRestClient = Mockito.mock(IssueRestClient.class);

    private final Promise<BasicIssue> issuePromise = Mockito.mock(Promise.class);

    private final BasicIssue basicIssue = Mockito.mock(BasicIssue.class);

    private static final String JIRA_URL = "http://localhost";
    private static final String JIRA_USERNAME = "username";
    private static final String JIRA_TOKEN = "token";
    private static final String JIRA_PPI_PROJECT_ID = "projectId";
    private static final String JIRA_PPI_ISSUE_TYPE_ID = "123456";
    private static final String ENVIRONMENT = "environment";

    private static final ArgumentCaptor<IssueInput> issueInputArgumentCaptor = ArgumentCaptor.forClass(IssueInput.class);
    private final JiraRestClientConfig jiraRestClientConfig = Mockito.spy(new JiraRestClientConfig(Map.of(
            "JIRA_URL", JIRA_URL,
            "JIRA_USERNAME", JIRA_USERNAME,
            "JIRA_TOKEN", JIRA_TOKEN,
            "JIRA_PPI_PROJECT_ID", JIRA_PPI_PROJECT_ID,
            "JIRA_PPI_ISSUE_TYPE_ID", JIRA_PPI_ISSUE_TYPE_ID,
            "ENVIRONMENT", ENVIRONMENT
    )));


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new JavaTimeModule());
    private final JsmIngestion function = new JsmIngestion(
            LoggerFactory.getLogger(getClass()),
            objectMapper,
            jiraRestClientConfig,
            jiraRestClient
    );


    @Test
    void runOk() throws JsonProcessingException {
        // test precondition
        Logger logger = Logger.getLogger("JsmIngestion-test-logger");
        given(context.getLogger()).willReturn(logger);
        given(jiraRestClient.getIssueClient()).willReturn(issueRestClient);
        given(issueRestClient.createIssue(issueInputArgumentCaptor.capture())).willReturn(issuePromise);
        given(issuePromise.claim()).willReturn(basicIssue);
        given(basicIssue.getKey()).willReturn("issueKey");
        String alertMessage = "{\"details\":{\"code\":\"TGP\",\"owner\":\"ownerTest\",\"threshold\":180.0,\"value\":150.0,\"triggerDate\":\"2023-09-11T17:30:06.207757+02:00\"}}";
        Alert alert = objectMapper.readValue(alertMessage, Alert.class);
        AlertDetails alertDetails = alert.getDetails();
        String expectedTicketDescription = """
                Alert details:
                KPI code: [%s]
                Owner: [%s]
                Threshold: [%s]
                Value: [%s]
                Alert triggering date: [%s]
                """.formatted(alertDetails.getCode(), alertDetails.getOwner(), alertDetails.getThreshold(), alertDetails.getValue(), alertDetails.getTriggerDate());
        String expectedSummary = "[QI - pagoPA] - [environment] A new KPI TGP alert was triggered for ownerTest";
        // test execution
        function.processJsmAlert(alertMessage, context);
        // test assertion -> this line means the call was successful
        IssueInput createdIssue = issueInputArgumentCaptor.getValue();
        verify(issueRestClient, times(1)).createIssue(any());
        verify(issuePromise, times(1)).claim();
        verify(basicIssue, times(1)).getKey();
        assertEquals(expectedSummary, createdIssue.getField(IssueFieldId.SUMMARY_FIELD.id).getValue());
        assertEquals(expectedTicketDescription, createdIssue.getField(IssueFieldId.DESCRIPTION_FIELD.id).getValue());
        assertEquals(JIRA_PPI_PROJECT_ID, ((ComplexIssueInputFieldValue) createdIssue.getField(IssueFieldId.PROJECT_FIELD.id).getValue()).getValuesMap().get("key"));
        assertEquals(JIRA_PPI_ISSUE_TYPE_ID, ((ComplexIssueInputFieldValue) createdIssue.getField(IssueFieldId.ISSUE_TYPE_FIELD.id).getValue()).getValuesMap().get("id"));
    }
}
