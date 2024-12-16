package io.rhdhorchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Ensure the registration of the CloudEvent jackson module according to the Quarkus suggested procedure.
 */
@Singleton
public class CloudEventsCustomizer implements ObjectMapperCustomizer {

  @Override
  public void customize(ObjectMapper mapper) {
    mapper.registerModule(JsonFormat.getCloudEventJacksonModule());
    mapper.registerModule(new JavaTimeModule());
  }
}
