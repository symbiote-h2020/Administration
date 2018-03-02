package eu.h2020.symbiote.administration.repository;

import eu.h2020.symbiote.model.mim.Federation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;


/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 3/1/2018.
 */
@RepositoryRestResource(collectionResourceRel = "federations", path = "federations")
public interface FederationRepository extends MongoRepository<Federation, String> {
    Optional<Federation> findById(String federationId);

    List<Federation> findAllByIsPublic(Boolean isPublic);

    List<Federation> deleteById(String id);
}
