package dev.parodos;

import dev.parodos.move2kube.ApiException;
import dev.parodos.service.FolderCreatorService;
import dev.parodos.service.GitService;
import dev.parodos.service.Move2KubeService;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@ApplicationScoped
public class SaveTransformationFunction {
  private static final Logger log = LoggerFactory.getLogger(SaveTransformationFunction.class);

  public static final String COMMIT_MESSAGE = "Move2Kube transformation";

  public static final String SOURCE = "save-transformation";

  public static final  String[] MOVE2KUBE_OUTPUT_DIRECTORIES_TO_SAVE = new String[]{"source","deploy","scripts" };

  @Inject
  GitService gitService;

  @Inject
  Move2KubeService move2KubeService;

  @Inject
  FolderCreatorService folderCreatorService;



  @Funq("saveTransformation")
  public CloudEvent<EventGenerator.EventPOJO> saveTransformation(FunInput input) {
    if (!input.validate()) {
      log.error("One or multiple mandatory input field was missing; input: {}", input);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("One or multiple mandatory input field was missing; input: %s", input),
          SOURCE);
    }

    Path transformationOutputPath;
    try {
      transformationOutputPath = move2KubeService.getTransformationOutput(input.workspaceId, input.projectId, input.transformId);
    } catch (IllegalArgumentException | IOException | ApiException e) {
      log.error("Error while retrieving transformation output", e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot get transformation output of transformation %s" +
              " in workspace %s for project %s for repo %s; error: %s",
          input.transformId, input.workspaceId, input.projectId, input.gitRepo, e), SOURCE);
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
      log.error("Error while creating httpclient to get transformation output", e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot create client to get transformation output of transformation %s" +
              " in workspace %s for project %s for repo %s; error: %s",
          input.transformId, input.workspaceId, input.projectId, input.gitRepo, e), SOURCE);
    }

    try {
      cleanTransformationOutputFolder(transformationOutputPath);
    } catch (IOException e) {
      log.error("Error while cleaning extracted transformation output", e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot clean extracted transformation output of transformation %s" +
              " in workspace %s for project %s for repo %s; error: %s",
          input.transformId, input.workspaceId, input.projectId, input.gitRepo, e), SOURCE);
    }

    return pushTransformationToGitRepo(input, transformationOutputPath);
  }


  private CloudEvent pushTransformationToGitRepo(FunInput input, Path transformationOutputPath) {
    try {
      Path gitDir = folderCreatorService.createGitRepositoryLocalFolder(input.gitRepo, String.format("%s-%s_%d", input.branch, input.transformId, new Date().getTime()));

      try (Git clonedRepo = gitService.cloneRepo(input.gitRepo, input.branch, gitDir)) {
        CloudEvent errorEvent = createBranch(input, clonedRepo);
        if (errorEvent != null) return errorEvent;

        try {
          moveTransformationOutputToBranchDirectory(transformationOutputPath, gitDir);
        } catch (IOException e) {
          log.error("Cannot move transformation output to local git repo " +
                  "(repo {}; transformation: {}; workspace: {}; project: {})",
              input.gitRepo, input.transformId, input.workspaceId, input.projectId, e);
          return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot move transformation output to local git repo " +
                  "(repo %s; transformation: %s; workspace: %s; project: %s); error: %s",
              input.gitRepo, input.transformId, input.workspaceId, input.projectId, e), SOURCE);
        }

        return commitAndPush(input, clonedRepo);

      } catch (GitAPIException e) {
        log.error("Cannot clone repo {}", input.gitRepo, e);
        return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot clone repo %s; error: %s", input.gitRepo, e),
            SOURCE);
      }
    } catch (IOException e) {
      log.error("Cannot create temp dir to clone repo {}", input.gitRepo, e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot create temp dir to clone repo %s; error: %s", input.gitRepo, e),
          SOURCE);
    }

  }

  public void moveTransformationOutputToBranchDirectory(Path transformationOutput, Path gitDirectory) throws IOException {
    cleanCurrentGitFolder(gitDirectory);
    log.info("Moving extracted files located in {} to git repo folder {}", transformationOutput, gitDirectory);
    Path finalPath;
    try (var dir = Files.newDirectoryStream(transformationOutput.resolve("output"), Files::isDirectory)) {
      finalPath = dir.iterator().next();
    }
    log.info("FinalPath is --->{} and GitPath is {}", finalPath, gitDirectory);
    for(String directory: MOVE2KUBE_OUTPUT_DIRECTORIES_TO_SAVE) {
      FileUtils.copyDirectory(finalPath.resolve(Paths.get(directory)).toFile(),
          gitDirectory.resolve(directory).toFile());
    }
    FileUtils.copyFile(finalPath.resolve(Paths.get("Readme.md")).toFile(),
        gitDirectory.resolve("Readme.md").toFile());
  }

  private static void cleanTransformationOutputFolder(Path transformationOutput) throws IOException {
    try (var files = Files.walk(transformationOutput)) {
      files.forEach(path -> {
        if (path.toAbsolutePath().toString().contains(".git") && path.toFile().isDirectory()) {
          try {
            FileUtils.deleteDirectory(path.toFile());
          } catch (IOException e) {
            log.error("Error while deleting .git directory {} of transformation output", path, e);
          }
        }
      });
    }
  }

  private static void cleanCurrentGitFolder(Path gitDirectory) throws IOException {
    try (var files = Files.walk(gitDirectory, 1)) {
      files.forEach(path -> {
        if (!path.equals(gitDirectory) && !path.toAbsolutePath().toString().contains(".git")) {
          File f = path.toFile();
          if (f.isDirectory()) {
            try {
              FileUtils.deleteDirectory(f);
            } catch (IOException e) {
              log.error("Error while deleting directory {}", path, e);
            }
          } else {
            try {
              FileUtils.delete(f);
            } catch (IOException e) {
              log.error("Error while deleting file {}", path, e);
            }
          }
        }
      });
    }
  }

  private CloudEvent createBranch(FunInput input, Git clonedRepo) {
    try {
      if (gitService.branchExists(clonedRepo, input.branch)) {
        log.error("Branch {} already exists on repo {}", input.branch, input.gitRepo);
        return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Branch '%s' already exists on repo %s",
            input.branch, input.gitRepo), SOURCE);
      }
      gitService.createBranch(clonedRepo, input.branch);
    } catch (GitAPIException e) {
      log.error("Cannot create branch {} to remote repo {}", input.branch, input.gitRepo, e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot create branch '%s' on repo %s; error: %s",
          input.branch, input.gitRepo, e), SOURCE);
    }
    return null;
  }

  private CloudEvent commitAndPush(FunInput input, Git clonedRepo) {
    try {
      gitService.commit(clonedRepo, COMMIT_MESSAGE, ".");
    } catch (GitAPIException e) {
      log.error("Cannot commit to local repo {}", input.gitRepo, e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot commit to local repo %s; error: %s", input.gitRepo, e),
          SOURCE);
    }
    log.info("Pushing commit to branch {} of repo {}", input.branch, input.gitRepo);
    try {
      gitService.push(clonedRepo);
    } catch (GitAPIException | IOException e) {
      log.error("Cannot push branch {} to remote repo {}", input.branch, input.gitRepo, e);
      return EventGenerator.createErrorEvent(input.workflowCallerId, String.format("Cannot push branch %s to remote repo %s; error: %s",
          input.branch, input.gitRepo, e), SOURCE);
    }

    var event = EventGenerator.createTransformationSavedEvent(input.workflowCallerId, SOURCE);
    log.info("Sending cloud event {} to workflow {}", event, input.workflowCallerId);
    return event;
  }


  public static class FunInput {
    public String gitRepo;
    public String branch;

    public String workspaceId;
    public String projectId;
    public String transformId;

    public String workflowCallerId;

    public boolean validate() {
      return !((gitRepo == null || gitRepo.isBlank()) ||
          (branch == null || branch.isBlank()) ||
          (workspaceId == null || workspaceId.isBlank()) ||
          (projectId == null || projectId.isBlank()) ||
          (workflowCallerId == null || workflowCallerId.isBlank()) ||
          (transformId == null || transformId.isBlank()));
    }

    @Override
    public String toString() {
      return "FunInput{" +
          "gitRepo='" + gitRepo + '\'' +
          ", branch='" + branch + '\'' +
          ", workspaceId='" + workspaceId + '\'' +
          ", projectId='" + projectId + '\'' +
          ", transformId='" + transformId + '\'' +
          ", workflowCallerId='" + workflowCallerId + '\'' +
          '}';
    }
  }

}
