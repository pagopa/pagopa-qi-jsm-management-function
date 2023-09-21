package it.pagopa.qi.jsmmanagement;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.DelegatingPromise;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;
import it.pagopa.qi.jsmmanagement.config.JiraRestClientConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JsmIngestionTest {

    @Spy
    JsmIngestion function;

    @Mock
    ExecutionContext context;

    @Mock
    JiraRestClient jiraRestClient;

    @Mock
    JiraRestClientConfig jiraRestClientConfig;

    @Mock
    IssueRestClient issueRestClient;

    @Test
    void runOk() throws URISyntaxException {
        // setting env var
        System.setProperty("JIRA_PPI_ISSUE_TYPE_ID", "10");
        System.setProperty("JIRA_PPI_PROJECT_ID", "20");
        // test precondition
        Logger logger = Logger.getLogger("JsmIngestion-test-logger");
        when(context.getLogger()).thenReturn(logger);

        when(jiraRestClientConfig.jiraRestClient()).thenReturn(jiraRestClient);
        when(jiraRestClient.getIssueClient()).thenReturn(issueRestClient);

        String result = "testResult";
        BasicIssue bs = new BasicIssue(new URI("bs"), "bs", 10L);
        Promise<BasicIssue> bsp = new Promise<BasicIssue>() {
            @Override
            public BasicIssue claim() {
                return bs;
            }

            @Override
            public Promise<BasicIssue> done(Consumer<? super BasicIssue> consumer) {
                return null;
            }

            @Override
            public Promise<BasicIssue> fail(Consumer<Throwable> consumer) {
                return null;
            }

            @Override
            public Promise<BasicIssue> then(TryConsumer<? super BasicIssue> tryConsumer) {
                return null;
            }

            @Override
            public <B> Promise<B> map(Function<? super BasicIssue, ? extends B> function) {
                return null;
            }

            @Override
            public <B> Promise<B> flatMap(Function<? super BasicIssue, ? extends Promise<? extends B>> function) {
                return null;
            }

            @Override
            public Promise<BasicIssue> recover(Function<Throwable, ? extends BasicIssue> function) {
                return null;
            }

            @Override
            public <B> Promise<B> fold(Function<Throwable, ? extends B> function, Function<? super BasicIssue, ? extends B> function1) {
                return null;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public BasicIssue get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public BasicIssue get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
        when(issueRestClient.createIssue(any(IssueInput.class))).thenReturn(bsp);

        String alertMessage = "{\"details\":{\"code\":\"TGP\",\"owner\":\"ownerTest\",\"threshold\":100.0,\"value\":150.0,\"triggerDate\":\"2023-09-11T17:30:06.207757+02:00\"}}";

        // test execution
        function.processJsmAlert(alertMessage, context);

        // test assertion -> this line means the call was successful
        assertTrue(true);
    }
}
