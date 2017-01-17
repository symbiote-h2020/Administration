package eu.h2020.symbiote;

import eu.h2020.symbiote.communication.IPlatformCreationResponseListener;
import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.PlatformCreationResponse;
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
 * Created by mateuszl on 22.09.2016.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AdministrationApplication {

    private static Log log = LogFactory.getLog(AdministrationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AdministrationApplication.class, args);
    }

    @Component
    public static class CLR implements CommandLineRunner, IPlatformCreationResponseListener {

        private final RabbitManager rabbitManager;

        @Autowired
        public CLR( RabbitManager rabbitManager) {
            this.rabbitManager = rabbitManager;
        }

        @Override
        public void run(String... args) throws Exception {
            this.rabbitManager.initCommunication();

//            //todo move (now for testing purposes)
//
//            Platform platform = new Platform();
//            platform.setName("p1");
//            platform.setDescription("d1");
//            platform.setInformationModelId("123");
//            platform.setUrl("http://123.com/");
//
//            this.rabbitManager.sendPlatformCreationRequest(platform, this);
        }

        @Override
        public void onPlatformCreationResponseReceive(PlatformCreationResponse platformCreationResponse) {
            System.out.println("Received response in interface: " + platformCreationResponse);
        }
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }
}
