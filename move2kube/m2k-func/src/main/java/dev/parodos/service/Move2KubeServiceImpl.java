package dev.parodos.service;

import dev.parodos.move2kube.ApiClient;
import dev.parodos.move2kube.ApiException;
import dev.parodos.move2kube.api.ProjectOutputsApi;
import dev.parodos.move2kube.api.ProjectsApi;
import dev.parodos.move2kube.client.model.Project;
import dev.parodos.move2kube.client.model.ProjectOutputsValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@ApplicationScoped
public class Move2KubeServiceImpl implements Move2KubeService {
  private static final Logger log = LoggerFactory.getLogger(Move2KubeServiceImpl.class);

  @Inject
  FolderCreatorService folderCreatorService;

  @ConfigProperty(name = "move2kube.api")
  String move2kubeApi;

  @Override
  public Path getTransformationOutput(String workspaceId, String projectId, String transformationId) throws IllegalArgumentException, IOException, ApiException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    Path outputPath = folderCreatorService.createMove2KubeTransformationFolder(String.format("move2kube-transform-%s", transformationId));
    ApiClient client = new ApiClient(createHttpClientAcceptingSelfSignedCerts());
    client.setBasePath(move2kubeApi);

    ProjectOutputsApi output = new ProjectOutputsApi(client);

    waitForTransformationToBeDone(workspaceId, projectId, transformationId, client);

    log.info("Retrieving transformation {} output (Workspace: {}, project: {}", workspaceId, projectId, transformationId);
    File file = output.getProjectOutput(workspaceId, projectId, transformationId);
    if (file == null) {
      log.error("Cannot get output file from transformation {} (Workspace: {}, project: {}", transformationId, workspaceId, projectId);
      throw new FileNotFoundException(String.format("Cannot get output file from transformation %s (Workspace: %s, project: %s", transformationId, workspaceId, projectId));
    }

    log.info("Extracting {} to {}", file.getAbsolutePath(), outputPath);
    extractZipFile(file, outputPath);

    return outputPath;
  }

  private void waitForTransformationToBeDone(String workspaceId, String projectId, String transformationId, ApiClient client) throws ApiException {
    log.info("Waiting for transformation {} to be done", transformationId);
    ProjectsApi project = new ProjectsApi(client);
    Project res ;
    ProjectOutputsValue o;
    do {
      res = project.getProject(workspaceId, projectId);
      o = res.getOutputs().get(transformationId);
      if (o == null) {
        log.error("Output is null for transformation {}", transformationId);
        throw new IllegalArgumentException(String.format("Cannot get the project (%s) transformation (%s) output from the list", projectId, transformationId));
      }
      log.info("Status of transformation Id {} is: {}", transformationId, o.getStatus());
      try {
        Thread.sleep(5000L);
      } catch (InterruptedException ignored) {

      }
    } while (!o.getStatus().equals("done")) ;
  }

  public static void extractZipFile(File zipFile, Path extractPath) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
      for (ZipArchiveEntry entry : Collections.list(zip.getEntries())) {
        File entryFile = new File(extractPath.toString() + "/" + entry.getName());
        if (entry.isDirectory()) {
          entryFile.mkdirs();
          continue;
        }
        File parentDir = entryFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
          parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(entryFile)) {
          zip.getInputStream(entry).transferTo(fos);
        }
      }
    }
  }

  private static CloseableHttpClient createHttpClientAcceptingSelfSignedCerts() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(new SSLContextBuilder().loadTrustMaterial(null, (TrustStrategy) (x509Certificates, s) -> true).build()
            )
            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .build())
        .build();
    return HttpClientBuilder
        .create()
        .setConnectionManager(connectionManager)
        .build();
  }
}
