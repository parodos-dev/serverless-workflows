package dev.parodos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.funqy.knative.events.CloudEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class EventServiceImpl implements EventService {
  public static final String BROKER_URL = ConfigProvider.getConfig().getValue("broker.url", String.class);
  private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void fireEvent(CloudEvent<EventGenerator.EventPOJO> event) throws IOException {
    String requestBody = objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(event.data());

    final HttpPost httpPost = new HttpPost(BROKER_URL);

    httpPost.setEntity(new ByteArrayEntity(requestBody.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON));
    httpPost.setHeader("Content-type", "application/json");
    httpPost.setHeader("ce-id", event.id());
    httpPost.setHeader("ce-source", event.source());
    httpPost.setHeader("ce-type", event.type());
    httpPost.setHeader("ce-specversion", "1.0");
    event.extensions().forEach((key, value) -> {
      httpPost.setHeader("ce-" + key.toLowerCase(), value);
    });
    log.info("Firing event type: {}, data: {}", event.type(), requestBody);

    try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse response = client.execute(httpPost)) {
      log.info("Message sent to {}: {}", BROKER_URL, response.getCode());
    }
  }
}
