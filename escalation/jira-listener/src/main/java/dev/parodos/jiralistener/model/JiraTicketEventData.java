package dev.parodos.jiralistener.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Builder
@Data
@Jacksonized
public class JiraTicketEventData {
    private String ticketId;
    private String workFlowInstanceId;
    private String workflowName;
    private String status;
}
