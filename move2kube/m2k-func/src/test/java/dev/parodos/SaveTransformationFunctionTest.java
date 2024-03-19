package dev.parodos;

import dev.parodos.move2kube.ApiException;
import dev.parodos.service.FolderCreatorService;
import dev.parodos.service.GitService;
import dev.parodos.service.Move2KubeService;
import dev.parodos.service.Move2KubeServiceImpl;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SaveTransformationFunctionTest {

  @InjectMock
  GitService gitServiceMock;

  @InjectMock
  Move2KubeService move2KubeServiceMock;

  @InjectMock
  FolderCreatorService folderCreatorService;

  @ConfigProperty(name = "transformation-saved.event.name")
  private String transformationSavedEventName;

  Path transformOutputPath;
  Path gitRepoLocalFolder;


  public static final String TRANSFORMED_ZIP = "SaveTransformationFunctionTest/references/transformation_output.zip";

  public static final String REFERENCE_FOLDER_ZIP = "SaveTransformationFunctionTest/references/expected_output.zip";

  public static  File REFERENCE_OUTPUT_UNZIP_PATH;

  ClassLoader classLoader = getClass().getClassLoader();

  private Git git;

  @BeforeEach
  public void setUp() throws GitAPIException, IOException {
    File tmpDir = Files.createTempDirectory("gitRepoTest").toFile();
    git = Git.init().setDirectory(tmpDir).call();
    REFERENCE_OUTPUT_UNZIP_PATH = Files.createTempDirectory("refOutput").toFile();
    Move2KubeServiceImpl.extractZipFile(new File(classLoader.getResource(REFERENCE_FOLDER_ZIP).getFile()), REFERENCE_OUTPUT_UNZIP_PATH.toPath());
  }

  @AfterEach
  public void tearDown() throws IOException {
    git.getRepository().close();
    if (transformOutputPath != null) {
      FileUtils.deleteDirectory(transformOutputPath.toFile());
    }
    if (gitRepoLocalFolder != null) {
      FileUtils.deleteDirectory(gitRepoLocalFolder.toFile());
    }
    if (REFERENCE_OUTPUT_UNZIP_PATH != null) {
      FileUtils.deleteDirectory(REFERENCE_OUTPUT_UNZIP_PATH);
    }
    transformOutputPath = null;
    gitRepoLocalFolder = null;
    REFERENCE_OUTPUT_UNZIP_PATH = null;
  }

  @Test
  public void testSaveTransformationIsWorking() throws GitAPIException, IOException, ApiException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));

    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenReturn(git);
    when(gitServiceMock.branchExists(any(), anyString())).thenReturn(false);
    doNothing().when(gitServiceMock).createBranch(eq(git), anyString());
    doNothing().when(gitServiceMock).commit(eq(git), anyString(), anyString());
    doNothing().when(gitServiceMock).createBranch(eq(git), anyString());
    doNothing().when(gitServiceMock).push(eq(git));

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", transformationSavedEventName)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(containsString("\"error\":null"));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(1)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(1)).branchExists(any(), anyString());
    verify(gitServiceMock, times(1)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(1)).push(eq(git));

    AssertFileMovedToGitLocalFolder(REFERENCE_OUTPUT_UNZIP_PATH.toPath());
  }

  @Test
  public void testSaveTransformationIsFailingWhenRetrievingTransformationOutput() throws IOException, ApiException, GitAPIException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenThrow(new IOException("Error while retrieving transformation output"));

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(0)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(0)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(0)).branchExists(any(), anyString());
    verify(gitServiceMock, times(0)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));
  }

  @Test
  public void testSaveTransformationGitCloneFail() throws GitAPIException, IOException, ApiException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenThrow(new InvalidRemoteException("Error while cloning repo"));

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(0)).branchExists(any(), anyString());
    verify(gitServiceMock, times(0)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(0)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));
  }

  @Test
  public void testSaveTransformationBranchExists() throws GitAPIException, IOException, ApiException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenReturn(git);
    when(gitServiceMock.branchExists(any(), anyString())).thenReturn(true);

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(1)).branchExists(any(), anyString());
    verify(gitServiceMock, times(0)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(0)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));
  }

  @Test
  public void testSaveTransformationCreateBranchFail() throws GitAPIException, IOException, ApiException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenReturn(git);
    when(gitServiceMock.branchExists(any(), anyString())).thenReturn(false);
    doThrow(new InvalidRemoteException("Error while creating new branch")).when(gitServiceMock).createBranch(eq(git), anyString());

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(1)).branchExists(any(), anyString());
    verify(gitServiceMock, times(1)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(0)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));
  }

  @Test
  public void testSaveTransformationCommitChangesFail() throws GitAPIException, IOException, ApiException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenReturn(git);
    when(gitServiceMock.branchExists(any(), anyString())).thenReturn(false);
    doNothing().when(gitServiceMock).createBranch(eq(git), anyString());
    doThrow(new InvalidRemoteException("Error while committing changes")).when(gitServiceMock).commit(eq(git), anyString(), anyString());

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(1)).branchExists(any(), anyString());
    verify(gitServiceMock, times(1)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(1)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));

    AssertFileMovedToGitLocalFolder(REFERENCE_OUTPUT_UNZIP_PATH.toPath());
  }

  @Test
  public void testSaveTransformationGitPushFails() throws GitAPIException, IOException, ApiException, URISyntaxException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    UUID transformId = UUID.randomUUID();
    transformOutputPath = Files.createTempDirectory(String.format("move2kube-transform-TEST-%s", transformId));
    gitRepoLocalFolder = Files.createTempDirectory(String.format("local-git-transform-TEST-%s", transformId));
    URL transformedZip = classLoader.getResource(TRANSFORMED_ZIP);
    Move2KubeServiceImpl.extractZipFile(new File(transformedZip.getFile()), transformOutputPath);

    when(folderCreatorService.createGitRepositoryLocalFolder(eq("gitRepo"), anyString())).thenReturn(gitRepoLocalFolder);
    when(move2KubeServiceMock.getTransformationOutput(anyString(), anyString(), anyString())).thenReturn(transformOutputPath);
    when(gitServiceMock.cloneRepo(anyString(), anyString(), any())).thenReturn(git);
    doNothing().when(gitServiceMock).commit(eq(git), anyString(), anyString());
    doNothing().when(gitServiceMock).createBranch(eq(git), anyString());
    doThrow(new InvalidRemoteException("Error while pushing to remote")).when(gitServiceMock).push(eq(git));

    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"workspaceId\": \"workspaceId\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"," +
            " \"transformId\": \"" + transformId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(1)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(1)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(1)).branchExists(any(), anyString());
    verify(gitServiceMock, times(1)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(1)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(1)).push(eq(git));

    AssertFileMovedToGitLocalFolder(REFERENCE_OUTPUT_UNZIP_PATH.toPath());
  }

  @Test
  public void testSaveTransformationMissingInput() throws GitAPIException, IOException, ApiException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    UUID workflowCallerId = UUID.randomUUID();
    RestAssured.given().contentType("application/json")
        .header("ce-specversion", "1.0")
        .header("ce-id", UUID.randomUUID().toString())
        .header("ce-type", "save-transformation")
        .header("ce-source", "test")
        .body("{\"gitRepo\": \"gitRepo\", " +
            "\"branch\": \"branch\"," +
            " \"projectId\": \"projectId\"," +
            " \"workflowCallerId\": \"" + workflowCallerId + "\"" +
            "}")
        .post("/")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header("ce-type", EventGenerator.ERROR_EVENT)
        .header("ce-kogitoprocrefid", workflowCallerId.toString())
        .header("ce-source", SaveTransformationFunction.SOURCE)
        .body(not(containsString("\"error\":null")));

    verify(move2KubeServiceMock, times(0)).getTransformationOutput(anyString(), anyString(), anyString());
    verify(gitServiceMock, times(0)).cloneRepo(anyString(), anyString(), any());
    verify(gitServiceMock, times(0)).branchExists(any(), anyString());
    verify(gitServiceMock, times(0)).createBranch(eq(git), anyString());
    verify(gitServiceMock, times(0)).commit(eq(git), anyString(), anyString());
    verify(gitServiceMock, times(0)).push(eq(git));
  }

  private void AssertFileMovedToGitLocalFolder(Path localOutputRef) {
    Path refDeploy = Path.of(localOutputRef.toString(), "/deploy");
    Path actualDeploy = Path.of(gitRepoLocalFolder.toString(), "/deploy");
    Path refScripts = Path.of(localOutputRef.toString(), "/scripts");
    Path actualScripts = Path.of(gitRepoLocalFolder.toString(), "/scripts");
    Path refSource = Path.of(localOutputRef.toString(), "/source");
    Path actualSource = Path.of(gitRepoLocalFolder.toString(), "/source");
    AssertDirsEqual(refDeploy.toFile(), actualDeploy.toFile());
    AssertDirsEqual(refScripts.toFile(), actualScripts.toFile());
    AssertDirsEqual(refSource.toFile(), actualSource.toFile());
  }

  public static void AssertDirsEqual(File ref, File actual) {
    Assertions.assertArrayEquals(
        FileUtils.listFilesAndDirs(ref, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream().map(File::getName).sorted().toArray(),
        FileUtils.listFilesAndDirs(actual, TrueFileFilter.TRUE, TrueFileFilter.TRUE).stream().map(File::getName).sorted().toArray());
  }
}
