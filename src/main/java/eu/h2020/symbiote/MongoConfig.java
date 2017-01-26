package eu.h2020.symbiote;

/**
 * Created by mateuszl on 30.09.2016.
 *
 * Note: to be used by components with MongoDB
 */

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories
class MongoConfig extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        return "symbiote-core-database";
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Mongo();
    }

    @Override
    protected String getMappingBasePackage() {
        return "com.oreilly.springdata.mongodb";
    }

    //TODO change localhost to sth read from configuration
    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new MongoClient("localhost"), "springsecurity");
    }

}