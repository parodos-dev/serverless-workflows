package io.rhdhorchestrator.service;

import io.rhdhorchestrator.move2kube.ApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public interface Move2KubeService {

  public Path getTransformationOutput(String workspaceId, String projectId, String transformationId) throws IllegalArgumentException, IOException, ApiException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;
}
