package dev.parodos.jiralistener;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.cloudevents.CloudEvent;
import io.cloudevents.jackson.JsonFormat;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/")
@RegisterRestClient(configKey="ce-emitter")
public interface EventNotifier {

    @POST
    @Produces(JsonFormat.CONTENT_TYPE)
    void emit(CloudEvent event);
}
