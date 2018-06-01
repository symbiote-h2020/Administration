package eu.h2020.symbiote.administration.repository;

import eu.h2020.symbiote.administration.model.CoreUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;


/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 3/1/2018.
 */
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends MongoRepository<CoreUser, String> {
    Optional<CoreUser> findByValidUsername(String validUsername);
}
