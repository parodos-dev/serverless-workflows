package dev.parodos.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class FolderCreatorServiceImpl implements FolderCreatorService {
  static final Logger log = LoggerFactory.getLogger(FolderCreatorServiceImpl.class);

  @Override
  public Path createGitRepositoryLocalFolder(String gitRepo, String uniqueIdentifier) throws IOException {
    String folder = String.format("local-git-transform-%s-%s", StringUtils.substringAfterLast(gitRepo, "/"), uniqueIdentifier);
    log.info("Creating temp folder: {}", folder);
    return Files.createTempDirectory(folder);
  }

  @Override
  public Path createMove2KubeTransformationFolder(String transformationId) throws IOException {
    String folder = String.format("move2kube-transform-%s", transformationId);
    log.info("Creating temp folder: {}", folder);
    return Files.createTempDirectory(folder);
  }

}
