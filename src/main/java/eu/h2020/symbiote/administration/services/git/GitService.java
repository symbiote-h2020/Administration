package eu.h2020.symbiote.administration.services.git;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

@Component
public class GitService implements IGitService {

    private static Log log = LogFactory.getLog(GitService.class);

    @Override
    public void cloneRepo(String repoUrl, String gitPath) throws Exception {
        log.info("Cloning " + repoUrl + " into " + gitPath);
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(Paths.get(gitPath).toFile())
                .call();
        log.info("Completed Cloning");
    }

    @Override
    public void pullRepo(String gitPath) throws Exception {
        log.info("Pulling in " + gitPath);

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(gitPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

        PullCommand pullCommand = new Git(repository).pull();
        pullCommand.setRemote("origin").call();
    }


}