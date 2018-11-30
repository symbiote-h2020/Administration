package eu.h2020.symbiote.administration.services.git;

import java.nio.file.Files;
import java.nio.file.Paths;

public interface IGitService {

    default void init(String repoUrl, String gitPath) throws Exception {

        if (Files.notExists(Paths.get(gitPath))) {
            cloneRepo(repoUrl, gitPath);
        } else {
            pullRepo(gitPath);
        }
    }

    void cloneRepo(String repoUrl, String gitPath) throws Exception;

    void pullRepo(String gitPath) throws Exception;
}
