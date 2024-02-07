package dev.parodos.jiralistener;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static dev.parodos.jiralistener.JiraConstants.FIELDS;
import static dev.parodos.jiralistener.JiraConstants.ISSUE;
import static dev.parodos.jiralistener.JiraConstants.KEY;
import static dev.parodos.jiralistener.JiraConstants.LABELS;
import static dev.parodos.jiralistener.JiraConstants.STATUS;
import static dev.parodos.jiralistener.JiraConstants.STATUS_CATEGORY;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Lists;

import dev.parodos.jiralistener.model.JiraTicketEventData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;

@QuarkusTest
public class JiraListenerResourceTest {
        private static WireMockServer sink;

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

        @Inject
        ObjectMapper mapper;

        @BeforeAll
        public static void startSink() {
                sink = new WireMockServer(options().port(8181));
                sink.start();
                sink.stubFor(post("/").willReturn(aResponse().withBody("ok").withStatus(200)));
        }

        @AfterAll
        public static void stopSink() {
                if (sink != null) {
                        sink.stop();
                }
        }

        @BeforeEach
        public void resetSink() {
                sink.resetRequests();
        }

        private Map<String, Object> aClosedIssue() {
                Map<String, Object> statusCategory = new HashMap<String, Object>(Map.of(KEY, "done"));
                Map<String, Object> status = new HashMap<String, Object>(Map.of(STATUS_CATEGORY, statusCategory));
                List<String> labels = new ArrayList<>(List.of(workflowInstanceIdJiraLabel + "=500",
                                workflowNameJiraLabel + "=" + expectedWorkflowName));
                Map<String, Object> fields = new HashMap<String, Object>(Map.of(LABELS, labels, STATUS, status));
                Map<String, Object> issue = new HashMap<String, Object>(Map.of(KEY, "PR-1", FIELDS, fields));
                return new HashMap<String, Object>(Map.of(ISSUE, issue));
        }

        @Test
        public void when_jiraIssueIsClosed_onEvent_returnsClosedTicket()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> webhookEvent = aClosedIssue();

                String workflowInstanceId = "500";
                JiraTicketEventData closedTicket = JiraTicketEventData.builder().ticketId("PR-1")
                                .workFlowInstanceId(workflowInstanceId)
                                .workflowName("escalation").status("done").build();

                ExtractableResponse<Response> response = given()
                                .when().contentType("application/json")
                                .body(webhookEvent).post("/webhook/jira")
                                .then()
                                .statusCode(200)
                                .extract();

                assertEquals(response.as(JiraTicketEventData.class), closedTicket, "Returns JiraTicketEventData");
                sink.verify(1, postRequestedFor(urlEqualTo("/"))
                                .withHeader("ce-source", WireMock.equalTo(cloudeventSource))
                                .withHeader("ce-type", WireMock.equalTo(cloudeventType))
                                .withHeader("ce-kogitoprocrefid", WireMock.equalTo(workflowInstanceId)));
                List<ServeEvent> allServeEvents = sink.getAllServeEvents();
                allServeEvents = Lists.reverse(allServeEvents);
                assertThat(allServeEvents, hasSize(1));

                ServeEvent event = allServeEvents.get(0);
                System.out.println("Received event with headers " + event.getRequest().getAllHeaderKeys());
                JiraTicketEventData eventBody = mapper.readValue(event.getRequest().getBody(), JiraTicketEventData.class);
                System.out.println("Received event with eventBody " + eventBody);
                assertThat(event.getRequest().header("ce-source").values().get(0),
                                is(cloudeventSource));
                assertThat(event.getRequest().header("ce-type").values().get(0),
                                is(cloudeventType));
                assertThat(event.getRequest().header("ce-kogitoprocrefid").values().get(0),
                                is(workflowInstanceId));
                assertThat("Response body is equal to the request body", eventBody, is(closedTicket));
        }

        @Test
        public void when_payloadIsInvalid_onEvent_returnsNoContent()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> invalidIssue = Map.of("invalid", "any");
                validateNoContentRequest(invalidIssue);

        }

        @Test
        public void when_jiraIssueHasNotAllRequiredFiels_onEvent_returnsNoContent()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> webhookEvent = aClosedIssue();
                Map<?, ?> issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((List<?>) ((Map<?, ?>) issue.get(FIELDS)).get(LABELS)).remove(0);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((List<?>) ((Map<?, ?>) issue.get(FIELDS)).get(LABELS)).remove(1);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((Map<?, ?>) issue.get(FIELDS)).remove(LABELS);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) issue.get(FIELDS)).get(STATUS)).get(STATUS_CATEGORY))
                                .remove(KEY);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((Map<?, ?>) ((Map<?, ?>) issue.get(FIELDS)).get(STATUS)).remove(STATUS_CATEGORY);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((Map<?, ?>) issue.get(FIELDS)).remove(STATUS);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                issue.remove(FIELDS);
                validateNoContentRequest(webhookEvent);

                webhookEvent = aClosedIssue();
                issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                issue.remove(KEY);
                validateNoContentRequest(webhookEvent);
        }

        @Test
        public void when_jiraIssueIsNotClosed_onEvent_returnsNoContent()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> webhookEvent = aClosedIssue();
                Map<?, ?> issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                ((Map<String, Object>) ((Map<?, ?>) ((Map<?, ?>) issue.get(FIELDS)).get(STATUS)).get(STATUS_CATEGORY))
                                .put(KEY,
                                                "undone");
                validateNoContentRequest(webhookEvent);
        }

        @Test
        public void when_jiraIssueHasWrongWorkflowName_onEvent_returnsNoContent()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> webhookEvent = aClosedIssue();
                Map<?, ?> issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                Map<String, Object> fields = ((Map<String, Object>) issue.get(FIELDS));
                fields.put(LABELS, List.of(workflowInstanceIdJiraLabel + "=500",
                                workflowNameJiraLabel + "=invalidName"));
                validateNoContentRequest(webhookEvent);
        }

        @Test
        public void when_jiraIssueHasWrongLabels_onEvent_returnsNoContent()
                        throws StreamReadException, DatabindException, IOException {
                Map<String, Object> webhookEvent = aClosedIssue();
                Map<?, ?> issue = (Map<?, ?>) webhookEvent.get(ISSUE);
                Map<String, Object> fields = ((Map<String, Object>) issue.get(FIELDS));
                fields.put(LABELS, List.of("anotherLabel"));
                validateNoContentRequest(webhookEvent);
        }

        private void validateNoContentRequest(Map<String, Object> issue) {
                ExtractableResponse<Response> response = given()
                                .when().contentType("application/json")
                                .body(issue).post("/webhook/jira")
                                .then()
                                .statusCode(204)
                                .extract();

                assertThat("Returns no content", response.asString(), is(""));
                sink.verify(0, postRequestedFor(urlEqualTo("/")));
                List<ServeEvent> allServeEvents = sink.getAllServeEvents();
                allServeEvents = Lists.reverse(allServeEvents);
                assertThat(allServeEvents, hasSize(0));
        }
}