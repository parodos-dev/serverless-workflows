package dev.parodos.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

@ApplicationScoped
public class GitServiceImpl implements GitService {
  private static final Logger log = LoggerFactory.getLogger(GitServiceImpl.class);
  public static final Path SSH_PRIV_KEY_PATH = Path.of(ConfigProvider.getConfig().getValue("ssh-priv-key-path", String.class));
  @Override
  public Git cloneRepo(String repo, String branch, Path targetDirectory) throws GitAPIException, IOException {
    try {
      if (repo.startsWith("ssh") && !repo.contains("@")) {
        log.info("No user specified in ssh git url, using 'git' user");
        String[] protocolAndHost = repo.split("://");
        String repoWithGitUser = "git@" + protocolAndHost[1];
        repo = protocolAndHost[0] + "://" + repoWithGitUser;
      }
      CloneCommand cloneCommand = Git.cloneRepository().setURI(repo).setDirectory(targetDirectory.toFile());
      log.info("Cloning repo {} in {} using ssh keys {}", repo, targetDirectory, SSH_PRIV_KEY_PATH);
      cloneCommand.setTransportConfigCallback(getTransport(SSH_PRIV_KEY_PATH));

      return cloneCommand.call();
    } catch (InvalidRemoteException e) {
      log.error("remote repository server '{}' is not available", repo, e);
      throw e;
    } catch (GitAPIException e) {
      log.error("Cannot clone repository: {}", repo, e);
      throw e;
    } catch (IOException e) {
      log.error("Cannot set ssh transport: {}", repo, e);
      throw e;
    }
  }

  @Override
  public void createBranch(Git repo, String branch) throws GitAPIException {
    log.info("Creating branch {} in repo {}", branch, repo.toString());
    repo.branchCreate().setName(branch).call();
    repo.checkout().setName(branch).call();
  }

  @Override
  public boolean branchExists(Git repo, String branch) throws GitAPIException {
    return repo.branchList()
        .setListMode(ListBranchCommand.ListMode.ALL)
        .call()
        .stream()
        .map(ref -> ref.getName())
        .anyMatch(branchName -> branchName.contains(branch));
  }

  @Override
  public void commit(Git repo, String commitMessage, String filePattern) throws GitAPIException {
    log.info("Committing files matching the pattern '{}' with message '{}' to repo {}", filePattern, commitMessage, repo);
    repo.add().setUpdate(true).addFilepattern(filePattern).call();
    repo.add().addFilepattern(filePattern).call();
    CommitCommand commit = repo.commit().setMessage(commitMessage);
    commit.setSign(Boolean.FALSE);
    commit.call();
  }

  @Override
  public void push(Git repo) throws GitAPIException, IOException {
    log.info("Pushing to repo {}", repo);
    PushCommand pushCommand = repo.push().setForce(false).setRemote("origin");

    log.info("Push using ssh key {}", SSH_PRIV_KEY_PATH);
    pushCommand.setTransportConfigCallback(getTransport(SSH_PRIV_KEY_PATH));

    pushCommand.call();
  }

  public static TransportConfigCallback getTransport(Path sshKeyPath) throws IOException {
    if (!sshKeyPath.toFile().exists()) {
      throw new IOException("SSH key file at '%s' does not exists".formatted(sshKeyPath.toString()));
    }

    var sshSessionFactory = new JschConfigSessionFactory() {
      @Override
      protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey");
      }

      @Override
      protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch defaultJSch = super.createDefaultJSch(fs);
        defaultJSch.removeAllIdentity();
        defaultJSch.addIdentity(sshKeyPath.toString());
        return defaultJSch;
      }
    };
    return new TransportConfigCallback() {
      @Override
      public void configure(Transport transport) {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);

      }
    };
  }
}
