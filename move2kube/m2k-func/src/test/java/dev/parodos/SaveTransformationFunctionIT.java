package dev.parodos;

import dev.parodos.move2kube.ApiException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(SaveTransformationFunctionIT.OverridePropertiesTestProfile.class)
public class SaveTransformationFunctionIT {
  @ConfigProperty(name = "transformation-saved.event.name")
  private String transformationSavedEventName;

  public static class OverridePropertiesTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "move2kube.api", "http://localhost:8080/api/v1"
      );
    }
  }

  @Test
  @Disabled
  // TODO: before each (or all?) create a workspoce and a project in move2kube local instance
  public void testSaveTransformationOK() throws GitAPIException, IOException, ApiException {
    UUID workflowCallerId = UUID.randomUUID();
    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"https://github.com/gabriel-farache/dotfiles\", " +
            "\"branch\": \"m2k-test\"," +
            " \"token\": \"<githubtoken>\"," +
            " \"workspaceId\": \"765a25fe-ab46-4aee-be7b-15d9b82da566\"," +
            " \"projectId\": \"dd4a8dc9-bc61-4047-a9b0-88bf43306d55\"," +
            " \"transformId\": \"b3350712-cac0-4a5c-a42d-355c5f9d1e5b\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", transformationSavedEventName)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(containsString("\"error\":null"));
  }
}
