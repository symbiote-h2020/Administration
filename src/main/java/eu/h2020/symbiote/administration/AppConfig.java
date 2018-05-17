package eu.h2020.symbiote.administration;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Collections;

@Configuration
@EnableMongoRepositories
public class AppConfig extends AbstractMongoConfiguration {

    @Value("${symbiote.core.administration.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Mongo mongo() {
        return new MongoClient();
    }

    @Override
    protected Collection<String> getMappingBasePackages() { return Collections.singletonList("com.oreilly.springdata.mongodb"); }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // The basename should be "classpath:/{path}/messages"
        messageSource.setBasename("classpath:/messages/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(0);
        return messageSource;
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster
                = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}