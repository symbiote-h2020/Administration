package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.model.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mateuszl on 09.01.2017.
 */
@Repository
public interface PlatformRepository extends MongoRepository<Platform, String>{
}
