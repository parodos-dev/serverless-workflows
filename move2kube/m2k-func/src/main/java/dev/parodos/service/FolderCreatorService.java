package dev.parodos.service;

import java.io.IOException;
import java.nio.file.Path;


public interface FolderCreatorService {


   Path createGitRepositoryLocalFolder(String gitRepo, String uniqueIdentifier) throws IOException;

   Path createMove2KubeTransformationFolder(String transformationId) throws IOException;
}
