package dev.parodos.jiralistener;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.parodos.jiralistener.model.JiraTicketEventData;
import dev.parodos.jiralistener.model.JiraIssue;
import dev.parodos.jiralistener.model.JiraIssue.StatusCategory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
public class JiraListenerService {
    @ConfigProperty(name = "cloudevent.type")
    String cloudeventType;
    @ConfigProperty(name = "cloudevent.source")
    String cloudeventSource;

    @ConfigProperty(name = "jira.webhook.label.workflowInstanceId")
    String workflowInstanceIdJiraLabel;
    @ConfigProperty(name = "jira.webhook.label.workflowName")
    String workflowNameJiraLabel;
    @ConfigProperty(name = "escalation.workflowName")
    String expectedWorkflowName;

    private Logger logger = System.getLogger(JiraListenerService.class.getName());

    @Inject
    @RestClient
    EventNotifier eventNotifier;

    @Inject
    ObjectMapper mapper;

    static class OnEventResponse {
        boolean eventAccepted;
        JiraTicketEventData jiraTicketEventData;
    }

    OnEventResponse onEvent(JiraIssue jiraIssue) {
        OnEventResponse response = new OnEventResponse();
        response.eventAccepted = false;

        Optional<JiraTicketEventData> ticket = validateIsAClosedJiraIssue(jiraIssue);
        if (ticket.isPresent()) {
            logger.log(Level.INFO, "Created ticket " + ticket.get());
            CloudEvent newCloudEvent = CloudEventBuilder.v1()
                    .withDataContentType(MediaType.APPLICATION_JSON)
                    .withExtension("kogitoprocrefid", ticket.get().getWorkFlowInstanceId())
                    .withId(UUID.randomUUID().toString())
                    .withType(cloudeventType)
                    .withSource(URI.create(cloudeventSource))
                    .withData(PojoCloudEventData.wrap(ticket.get(),
                            mapper::writeValueAsBytes))
                    .build();

            logger.log(Level.INFO, "Emitting " + newCloudEvent);
            eventNotifier.emit(newCloudEvent);
            response.eventAccepted = true;
            response.jiraTicketEventData = ticket.get();
        }

        return response;
    }

    private Optional<JiraTicketEventData> validateIsAClosedJiraIssue(JiraIssue jiraIssue) {
        Optional<JiraTicketEventData> notaClosedJiraIssue = Optional.empty();
        String issueKey = jiraIssue.getKey();
        if (jiraIssue.getKey() != null) {
            if (jiraIssue.getFields() == null) {
                logger.log(Level.WARNING, "Discarded because of missing field: issue.fields");
                return notaClosedJiraIssue;
            }

            if (jiraIssue.getFields().getLabels() == null) {
                logger.log(Level.WARNING, String.format("Discarded because of missing field: issue.fields.labels"));
                return notaClosedJiraIssue;
            }
            List<String> labels = jiraIssue.getFields().getLabels();

            Optional<String> workflowInstanceIdLabel = labels.stream()
                    .filter(l -> l.startsWith(workflowInstanceIdJiraLabel + "=")).findFirst();
            if (workflowInstanceIdLabel.isEmpty()) {
                logger.log(Level.INFO,
                        String.format("Discarded because no %s label found", workflowInstanceIdJiraLabel));
                return notaClosedJiraIssue;
            }
            String workflowInstanceId = workflowInstanceIdLabel.get().split("=")[1];

            Optional<String> workflowNameLabel = labels.stream()
                    .filter(l -> l.startsWith(workflowNameJiraLabel + "=")).findFirst();
            if (workflowNameLabel.isEmpty()) {
                logger.log(Level.INFO, String.format("Discarded because no %s label found", workflowNameJiraLabel));
                return notaClosedJiraIssue;
            }
            String workflowName = workflowNameLabel.get().split("=")[1];
            if (!workflowName.equals(expectedWorkflowName)) {
                logger.log(Level.INFO,
                        String.format("Discarded because label %s is not matching the expected value %s",
                                workflowNameLabel.get(), expectedWorkflowName));
                return notaClosedJiraIssue;
            }

            if (jiraIssue.getFields().getStatus() == null) {
                logger.log(Level.WARNING, String.format("Discarded because of missing field: issue.fields.status"));
                return notaClosedJiraIssue;
            }
            JiraIssue.Status status = jiraIssue.getFields().getStatus();

            if (status.getStatusCategory() == null) {
                logger.log(Level.WARNING,
                        String.format("Discarded because of missing field: issue.fields.status.statusCategory"));
                return notaClosedJiraIssue;
            }
            StatusCategory statusCategory = status.getStatusCategory();

            if (statusCategory.getKey() == null) {
                logger.log(Level.WARNING,
                        String.format("Discarded because of missing field: issue.fields.status.statusCategory.key"));
                return notaClosedJiraIssue;
            }
            String statusCategoryKey = statusCategory.getKey();

            logger.log(Level.INFO,
                    String.format("Received Jira issue %s with workflowInstanceId %s, workflowName %s and status %s",
                            issueKey,
                            workflowInstanceId, workflowName, statusCategoryKey));
            if (!statusCategoryKey.equals("done")) {
                logger.log(Level.INFO, "Discarded because not a completed issue but " + statusCategoryKey);
                return notaClosedJiraIssue;
            }

            return Optional.of(JiraTicketEventData.builder().ticketId(issueKey)
                    .workFlowInstanceId(workflowInstanceId)
                    .workflowName(workflowName).status(statusCategoryKey).build());
        } else {
            logger.log(Level.INFO, "Discarded because of missing field: key");
            return notaClosedJiraIssue;
        }
    }
}
