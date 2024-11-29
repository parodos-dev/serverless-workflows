package dev.parodos.jiralistener.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEvent {
    private String timestamp;
    private String webhookEvent;
    private String issue_event_type_name;

    private JiraIssue issue;
}
