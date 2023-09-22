package it.pagopa.qi.jsmmanagement.config;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import lombok.Data;

import java.net.URI;
import java.util.Map;

@Data
public class JiraRestClientConfig {

    private final String jiraUrl;

    private final String username;

    private final String token;

    private final String ppiProjectId;

    private final long ppiIssueTypeId;

    private final String environment;

    /**
     * Constructor
     */
    public JiraRestClientConfig() {
        this.jiraUrl = System.getenv("JIRA_URL");
        this.username = System.getenv("JIRA_USERNAME");
        this.token = System.getenv("JIRA_TOKEN");
        this.ppiProjectId = System.getenv("JIRA_PPI_PROJECT_ID");
        this.ppiIssueTypeId = Long.parseLong(System.getenv("JIRA_PPI_ISSUE_TYPE_ID"));
        this.environment = System.getenv("ENVIRONMENT");
    }

    /**
     * Constructor with custom env
     *
     * @param env - {@link String} key-value map used to override env
     */
    public JiraRestClientConfig(Map<String, String> env) {
        this.jiraUrl = env.get("JIRA_URL");
        this.username = env.get("JIRA_USERNAME");
        this.token = env.get("JIRA_TOKEN");
        this.ppiProjectId = env.get("JIRA_PPI_PROJECT_ID");
        this.ppiIssueTypeId = Long.parseLong(env.get("JIRA_PPI_ISSUE_TYPE_ID"));
        this.environment = env.get("ENVIRONMENT");
    }


    /**
     * Build Jira asynchronous rest client
     *
     * @return the {@link JiraRestClient} instance
     */
    public JiraRestClient jiraRestClient() {
        return new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(URI.create(jiraUrl), username, token);
    }

}
