package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

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
public class AdministrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdministrationApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner {

        private final RabbitManager rabbitManager;

        @Autowired
        public CLR(RabbitManager rabbitManager) {
            this.rabbitManager = rabbitManager;
        }

        @Override
        public void run(String... args) throws Exception {
            this.rabbitManager.initCommunication();
        }
    }

}
