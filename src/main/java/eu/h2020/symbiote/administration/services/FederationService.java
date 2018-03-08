package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.repository.FederationRepository;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.model.mim.FederationMember;
import eu.h2020.symbiote.model.mim.InformationModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FederationService {
    private static Log log = LogFactory.getLog(FederationService.class);

    private FederationRepository federationRepository;
    private PlatformService platformService;
    private InformationModelService informationModelService;
    private ValidationService validationService;

    @Autowired
    public FederationService(FederationRepository federationRepository,
                             PlatformService platformService,
                             InformationModelService informationModelService,
                             ValidationService validationService) {

        Assert.notNull(federationRepository,"FederationRepository can not be null!");
        this.federationRepository = federationRepository;

        Assert.notNull(platformService,"PlatformService can not be null!");
        this.platformService = platformService;

        Assert.notNull(informationModelService,"InformationModelService can not be null!");
        this.informationModelService = informationModelService;

        Assert.notNull(validationService,"ValidationService can not be null!");
        this.validationService = validationService;
    }


    public ResponseEntity<?> listFederations() {
        // Todo: limit the results only to public federations?
        Map<String, Federation> federationMap = federationRepository.findAll().stream()
                .collect(Collectors.toMap(Federation::getId, federation -> federation));
        return new ResponseEntity<>(federationMap, new HttpHeaders(), HttpStatus.OK);
    }

    public ResponseEntity<?> createFederation(Federation federation,
                                              BindingResult bindingResult) {

        Map<String, Object> responseBody = new HashMap<>();

        if (bindingResult.hasErrors())
            return validationService.getRequestErrors(bindingResult);

        Optional<Federation> existingFederation = federationRepository.findById(federation.getId());
        if (existingFederation.isPresent()) {
            responseBody.put("error", "The federation with id '" + federation.getId() +
                    "' already exists!");
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        // Checking if all the platform members exist
        for (FederationMember member : federation.getMembers()) {
            ResponseEntity registryResponse = platformService.getPlatformDetailsFromRegistry(member.getPlatformId());

            if (registryResponse.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                responseBody.put("error", registryResponse.getBody());
                return new ResponseEntity<>(responseBody, new HttpHeaders(), registryResponse.getStatusCode());

            } else if (registryResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                responseBody.put("error", "The platform with id " + member.getPlatformId() + " was not found");
                return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);

            }

            member.setInterworkingServiceURL(((PlatformRegistryResponse) registryResponse.getBody())
                    .getBody().getInterworkingServices().get(0).getUrl());
        }

        // Checking if the information model exist
        ResponseEntity informationModelsResponse = informationModelService.getInformationModels();
        if (informationModelsResponse.getStatusCode() != HttpStatus.OK) {
            responseBody.put("error", informationModelsResponse.getBody());
            return new ResponseEntity<>(responseBody, new HttpHeaders(), informationModelsResponse.getStatusCode());
        }

        List<String> informationModels = ((List<InformationModel>) informationModelsResponse.getBody()).stream()
                .map(InformationModel::getId).collect(Collectors.toList());

        if (!informationModels.contains(federation.getInformationModel().getId())) {
            responseBody.put("error", "The information model with id " + federation.getInformationModel().getId()
                    + " was not found");
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        // Todo: Inform the Federation Managers of the platform members

        // Storing the new federation
        federation = federationRepository.save(federation);
        responseBody.put("message", "Federation Registration was successful!");
        responseBody.put("federation", federation);
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.CREATED);
    }

    public ResponseEntity<?> deleteFederation(String federationIdToDelete) {

        log.debug("POST request on /administration/user/cpanel/delete_federation for federation with id = " + federationIdToDelete);

        List<Federation> deletedFederations = federationRepository.deleteById(federationIdToDelete);
        Map<String, Object> response = new HashMap<>();

        // Todo: Inform the Federation Managers of the platform members

        if (deletedFederations.size() > 0) {
            response.put(deletedFederations.get(0).getId(), deletedFederations.get(0));
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
        }
        else {
            response.put("error", "The federation was not found");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<?> leaveFederation(String federationId, String platformId, Principal principal, boolean isAdmin) {
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        Map<String, Object> responseBody = new HashMap<>();

        if (!isAdmin) {
            // Check if the user owns the platform
            ResponseEntity<?> ownedPlatformDetailsResponse = platformService.checkIfUserOwnsPlatform(platformId, user);
            if (ownedPlatformDetailsResponse.getStatusCode() != HttpStatus.OK)
                return ownedPlatformDetailsResponse;
        }

        Optional<Federation> federation = federationRepository.findById(federationId);
        if (!federation.isPresent()) {
            responseBody.put("error", "The federation does not exist");
            return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }

        ResponseEntity<?> isPlatformMember =  isPlatformMemberOfFederation(federation.get(), platformId);
        if (isPlatformMember.getStatusCode() != HttpStatus.OK)
            return isPlatformMember;

        int memberIndex = (Integer) isPlatformMember.getBody();


        ResponseEntity<?> isPlatformTheOnlyMember =  isPlatformTheOnlyMemberOfFederation(federation.get(), platformId);
        if (isPlatformTheOnlyMember.getStatusCode() != HttpStatus.OK)
            return isPlatformTheOnlyMember;

        // Remove platform member
        federation.get().getMembers().remove(memberIndex);

        // Todo: Inform the Federation Managers of the platform members
        federationRepository.save(federation.get());

        responseBody.put(federation.get().getId(), federation.get());
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.OK);
    }

    private ResponseEntity<?> isPlatformMemberOfFederation(Federation federation, String platformId) {

        // If found, return the index of the platform member
        for (int i = 0; i < federation.getMembers().size(); i++) {
            if (federation.getMembers().get(i).getPlatformId().equals(platformId))
                return new ResponseEntity<>(i, new HttpHeaders(), HttpStatus.OK);
        }

        Map<String, Object> responseBody = new HashMap<>();
        String message = "Platform " + platformId + " is not a member of federation " + federation.getId();
        log.warn(message);
        responseBody.put("error", message);
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);

    }

    private ResponseEntity<?> isPlatformTheOnlyMemberOfFederation(Federation federation, String platformId) {
        if (federation.getMembers().size() > 1)
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);

        Map<String, Object> responseBody = new HashMap<>();
        String message = "Platform " + platformId + " is the only a member of federation " + federation.getId() +
                ". Please, delete the federation";
        log.warn(message);
        responseBody.put("error", message);
        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);

    }
}
