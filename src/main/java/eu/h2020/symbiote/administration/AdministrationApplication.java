package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.services.git.GitService;
import eu.h2020.symbiote.administration.services.git.IGitService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Spring Boot Application class for Administration (AAM) component.
 *
 * Administration is responsible for registering platforms and apps by platform owners and users. 
 * It provides a web-based GUI for performing platform operations.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 * @author Artur Jaworski (PSNC)
 */
@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@SpringBootApplication
@EnableMongoRepositories
public class AdministrationApplication {

    private static Log log = LogFactory.getLog(AdministrationApplication.class);

    public static void main(String[] args) {
        WaitForPort.waitForServices(WaitForPort.findProperty("SPRING_BOOT_WAIT_FOR_SERVICES"));
        SpringApplication.run(AdministrationApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;
        private final IGitService gitService;
        private final String cloudConfigGitPath;
        private final String cloudConfigRepoUrl;
        private final String enablerConfigGitPath;
        private final String enablerConfigRepoUrl;

        @Autowired
        public CLR(RabbitManager rabbitManager, IGitService gitService,
                   @Value("${symbiote.core.cloudconfig.git.path}") String cloudConfigGitPath,
                   @Value("${symbiote.core.cloudconfig.repo.url}") String cloudConfigRepoUrl,
                   @Value("${symbiote.core.enablerconfig.git.path}") String enablerConfigGitPath,
                   @Value("${symbiote.core.enablerconfig.repo.url}") String enablerConfigRepoUrl) {

            this.rabbitManager = rabbitManager;
            this.gitService = gitService;

            Assert.notNull(cloudConfigGitPath,"cloudConfigGitPath can not be null!");
            this.cloudConfigGitPath = cloudConfigGitPath;

            Assert.notNull(cloudConfigRepoUrl,"cloudConfigRepoUrl can not be null!");
            this.cloudConfigRepoUrl = cloudConfigRepoUrl;

            Assert.notNull(enablerConfigGitPath,"enablerConfigGitPath can not be null!");
            this.enablerConfigGitPath = enablerConfigGitPath;

            Assert.notNull(enablerConfigRepoUrl,"enablerConfigRepoUrl can not be null!");
            this.enablerConfigRepoUrl = enablerConfigRepoUrl;

        }

        @Override
        public void run(String... args) throws Exception {
            try {
                this.rabbitManager.initCommunication();
                this.gitService.init(cloudConfigRepoUrl, cloudConfigGitPath);
                this.gitService.init(enablerConfigRepoUrl, enablerConfigGitPath);
            } catch (Exception e) {
                log.error("Exception thrown during initialization of Administration", e);
                throw e;
            }
        }
    }
}
