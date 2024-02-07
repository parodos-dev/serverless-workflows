package dev.parodos.jiralistener;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.parodos.jiralistener.JiraListenerService.OnEventResponse;
import dev.parodos.jiralistener.model.JiraIssue;
import dev.parodos.jiralistener.model.WebhookEvent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/")
public class JiraListenerResource {
    private Logger logger = System.getLogger(JiraListenerResource.class.getName());

    @Inject
    ObjectMapper mapper;

    @Inject
    JiraListenerService jiraListenerService;

    // Test endpoint used in dev mode when not specifying a K_SINK variable
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public void test(Object any) {
        logger.log(Level.INFO, "RECEIVED " + any);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/webhook/jira")
    public Response onEvent(Map<String, Object> requestBody) {
        logger.log(Level.INFO, "Received " + requestBody);

        try {
            WebhookEvent webhookEvent = mapper.readValue(mapper.writeValueAsBytes(requestBody), WebhookEvent.class);
            logger.log(Level.INFO, "Received " + webhookEvent);
            if (webhookEvent.getIssue() == null) {
                logger.log(Level.WARNING, "Discarded because of missing field: issue");
                return Response.noContent().build();
            }
            JiraIssue jiraIssue = webhookEvent.getIssue();

            OnEventResponse response = jiraListenerService.onEvent(jiraIssue);
            if (response.eventAccepted) {
                return Response.ok(response.jiraTicketEventData).build();
            }
            return Response.noContent().build();
        } catch (IOException e) {
            return Response
                    .status(Status.BAD_REQUEST.getStatusCode(),
                            "Not a valid webhook event for a Jira issue: " + e.getMessage())
                    .build();
        }
    }
}