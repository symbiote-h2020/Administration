package eu.h2020.symbiote;

import eu.h2020.symbiote.communication.RabbitManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


/**
 * Administration module's entry point.
 * <p>
 * Administration is responsible for registering platforms by platform owners.
 * It provides a web-based GUI for performing platform operations.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AdministrationApplication {

    private static Log log = LogFactory.getLog(AdministrationApplication.class);

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

//            For testing purpopses

//            Platform platform = new Platform();
//            platform.setPlatformId("5886154b3999e53d70b46589");
//            platform.setName("New Platform changed");
// //            platform.setDescription("Test platform to delete");
// //            platform.setInformationModelId("CIM");
// //            platform.setUrl("http://example.com");

//            this.rabbitManager.sendPlatformCreationRequest(platform, rpcPlatformResponse ->
//                    log.debug("Received response in interface: " + rpcPlatformResponse));

            //            this.rabbitManager.sendPlatformRemovalRequest(platform, rpcPlatformResponse ->
            //                    log.debug("Platform deleted response: " + rpcPlatformResponse));

//             this.rabbitManager.sendPlatformModificationRequest(platform, rpcPlatformResponse ->
//                     log.debug("Platform modification response: " + rpcPlatformResponse));
        }
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }
}
