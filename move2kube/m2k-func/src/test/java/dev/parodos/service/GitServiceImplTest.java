package dev.parodos.service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Comparator;

import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider;
import org.apache.sshd.git.GitLocationResolver;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class GitServiceImplTest {
    static SshServer sshd;
    static Path serverDir;
    static Path cloneDir;
    static final String testFile = "test-file";
    static final String testFileContent = "test-content";
    GitServiceImpl underTest = new GitServiceImpl();

    @Test
    void cloneRepo() throws IOException, GitAPIException {
        try (Git git = underTest.cloneRepo("ssh://%s:%d%s".formatted(sshd.getHost(), sshd.getPort(), serverDir.toString()), "", cloneDir)) {
            Path testfile = cloneDir.resolve(testFile);
            Assertions.assertTrue(Files.exists(testfile), "cloned file doesn't exists");
            Assertions.assertEquals(Files.readString(testfile), testFileContent);
        }
    }

    @Test
    void push() throws IOException, GitAPIException {
        try (Git git = underTest.cloneRepo("ssh://%s:%d%s".formatted(sshd.getHost(), sshd.getPort(), serverDir.toString()), "", cloneDir)) {
            Files.write(cloneDir.resolve("push-test-file"), "foo".getBytes());
            underTest.commit(git, "push test commit", ".");
            Assertions.assertDoesNotThrow(() -> underTest.push(git));
        }
    }

    @BeforeAll
    static void init() throws IOException, GitAPIException {
        // create an ssh server
        sshd = SshServer.setUpDefaultServer();
        sshd.setHost("localhost");
        sshd.setPort(30022);
        URL hostkey = GitServiceImplTest.class.getClassLoader().getResource("m2k-test-ssh-id_ed25519.pub");
        SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider(Paths.get(hostkey.getPath()));
        sshd.setKeyPairProvider(keyPairProvider);
        keyPairProvider.loadKeys(null);
        sshd.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);

        serverDir = Files.createTempDirectory(
                "m2kfunc-test-git-repo-server",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));

        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sshd.setCommandFactory(new GitPackCommandFactory(GitLocationResolver.constantPath(Paths.get("/"))));
        sshd.setKeyPairProvider(new ClassLoadableResourceKeyPairProvider("test.key"));
        sshd.setPublickeyAuthenticator((username, key, session) -> true);
        sshd.start();

        // crete git repo on the ssh server
        Repository repo = new FileRepositoryBuilder().setWorkTree(serverDir.toFile()).build();
        repo.create(true);
        Files.write(serverDir.resolve(testFile).toAbsolutePath(), testFileContent.getBytes());
        new AddCommand(repo).addFilepattern(".").call();
        Git git = new Git(repo);
        git.add().addFilepattern(".").call();
        git.commit().setAuthor("me", "me@me").setMessage("first commit from test init").call();
    }

    @AfterAll
    static void afterAll() throws IOException {
        try (var walk = Files.walk(serverDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @BeforeEach
    void setup() throws IOException {
        cloneDir = Files.createTempDirectory(
                "m2kfunc-test-git-repo-clone",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
    }

    @AfterEach
    void teardown() throws IOException {
        try (var walk = Files.walk(cloneDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

}