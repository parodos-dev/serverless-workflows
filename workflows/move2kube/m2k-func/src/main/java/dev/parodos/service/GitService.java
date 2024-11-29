package dev.parodos.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

public interface GitService {

  // Clone the git repository locally on the `targetDirectory` folder
  Git cloneRepo(String repo, String branch, Path targetDirectory) throws GitAPIException, IOException;

  // Clone then archive the git repository. The archive is saved as `archiveOutputPath`.
  // The repository is locally persisted when cloned in the parent directory of `archiveOutputPath`

  void createBranch(Git repo, String branch) throws GitAPIException;

  void commit(Git repo, String commitMessage, String filePattern) throws GitAPIException;

  void push(Git repo) throws GitAPIException, IOException;

  // Check is a branch exists on the repository based on the cloned git repository persisted in the directory `gitDir`
  public boolean branchExists(Git repo, String branch) throws GitAPIException;
}
