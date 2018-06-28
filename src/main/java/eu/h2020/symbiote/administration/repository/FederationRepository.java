package eu.h2020.symbiote.administration.repository;

import eu.h2020.symbiote.administration.model.FederationWithInvitations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;


/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 3/1/2018.
 */
@RepositoryRestResource(collectionResourceRel = "federations", path = "federations")
public interface FederationRepository extends MongoRepository<FederationWithInvitations, String> {
    Optional<FederationWithInvitations> findById(String federationId);

    List<FederationWithInvitations> findAllByIsPublic(Boolean isPublic);

    Optional<FederationWithInvitations> deleteById(String id);

    @Query("{'members.platformId' : ?0}")
    List<FederationWithInvitations> findAllByPlatformMember(String platformId);
}
