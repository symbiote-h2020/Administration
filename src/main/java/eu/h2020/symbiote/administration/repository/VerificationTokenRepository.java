package eu.h2020.symbiote.administration.repository;

import eu.h2020.symbiote.administration.model.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;


/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 3/1/2018.
 */
@RepositoryRestResource(collectionResourceRel = "federations", path = "federations")
public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {
    Optional<VerificationToken> findByToken(String token);

    void deleteByUser_ValidUsername(String username);
}
