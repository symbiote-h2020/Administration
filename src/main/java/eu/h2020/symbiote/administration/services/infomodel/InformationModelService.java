package eu.h2020.symbiote.administration.services.infomodel;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericHttpErrorException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.core.cci.InfoModelMappingRequest;
import eu.h2020.symbiote.core.cci.InfoModelMappingResponse;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.*;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.OntologyMapping;
import eu.h2020.symbiote.semantics.mapping.model.Mapping;
import eu.h2020.symbiote.semantics.mapping.parser.ParseException;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class InformationModelService {
    private static Log log = LogFactory.getLog(InformationModelService.class);

    private RabbitManager rabbitManager;

    @Autowired
    public InformationModelService(RabbitManager rabbitManager) {
        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;
    }

    public ResponseEntity<?> listUserInformationModels(Principal principal) {
        log.trace("listUserInformationModels");

        // Checking if the user owns the platform
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {
            ArrayList<InformationModel> userInfoModels = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<InformationModel> informationModels = (List<InformationModel>) responseEntity.getBody();

            for (InformationModel informationModel : informationModels) {
                if (informationModel.getOwner().equals(user.getUsername()))
                    userInfoModels.add(informationModel);
            }
            return new ResponseEntity<>(userInfoModels, new HttpHeaders(), HttpStatus.OK);
        }
    }

    public ResponseEntity<?> registerInformationModel(String name, String uri, MultipartFile rdfFile,
                                                      Principal principal) {

        log.trace("registerInformationModel");

        UrlValidator urlValidator = new UrlValidator();

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        Map<String, String> response = new HashMap<>();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        if (name.length() < 2 || name.length() > 30)
            response.put("info_model_reg_error_name", "The name should have from 2 to 30 characters");
        if (!urlValidator.isValid(uri))
            response.put("info_model_reg_error_uri", "The uri is invalid");
        if (!rdfFile.getOriginalFilename().matches("^[\\w.]+\\.(ttl|nt|rdf|xml|n3|jsonld)$"))
            response.put("info_model_reg_error_rdf", "This format is not supported");

        try {
            log.debug("The size of the file is " + rdfFile.getBytes().length + "bytes");
        } catch (IOException e) {
            log.info("", e);
        }

        if (response.size() > 0) {
            response.put("error", "Invalid Arguments");
            return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        InformationModelResponse registryResponse;
        try {
            InformationModel informationModel = new InformationModel();
            informationModel.setName(name);
            informationModel.setOwner(user.getUsername());
            informationModel.setUri(uri);
            informationModel.setRdf(new String(rdfFile.getBytes(), StandardCharsets.UTF_8));

            String[] parts = rdfFile.getOriginalFilename().split("\\.");
            informationModel.setRdfFormat(RDFFormat.fromFilenameExtension(parts[parts.length-1]));


            InformationModelRequest request = new InformationModelRequest();
            request.setBody(informationModel);

            registryResponse = rabbitManager.sendRegisterInfoModelRequest(request);
            if (registryResponse != null) {
                if (registryResponse.getStatus() != HttpStatus.OK.value()) {
                    String message = "Registry responded with: " + registryResponse.getStatus();
                    log.info(message);
                    response.put("error", registryResponse.getMessage());
                    return new ResponseEntity<>(response,
                            new HttpHeaders(), HttpStatus.valueOf(registryResponse.getStatus()));
                }
            } else {
                String message = "Registry unreachable!";
                log.warn(message);
                response.put("error", message);
                return new ResponseEntity<>(response,
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            String message = "Could not read the rdfFile";
            log.warn(message);
            response.put("error", message);
            return new ResponseEntity<>(response,
                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(registryResponse.getBody(), new HttpHeaders(), HttpStatus.CREATED);
    }

    public ResponseEntity<?> deleteInformationModel(String infoModelIdToDelete, Principal principal) {

        log.trace("deleteInformationModel");

        // Checking if the user owns the information model
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {
            @SuppressWarnings("unchecked")
            List<InformationModel> informationModels = (List<InformationModel>) responseEntity.getBody();

            for (InformationModel informationModel : informationModels) {
                log.debug(informationModel.getId() + " " + informationModel.getOwner());
                if (informationModel.getId().equals(infoModelIdToDelete) &&
                        informationModel.getOwner().equals(user.getUsername())) {

                    // Ask Registry
                    try {
                        InformationModelRequest request = new InformationModelRequest();
                        request.setBody(informationModel);

                        InformationModelResponse response = rabbitManager.sendDeleteInfoModelRequest(request);
                        if (response != null) {
                            if (response.getStatus() != HttpStatus.OK.value()) {

                                return new ResponseEntity<>(response.getMessage(),
                                        new HttpHeaders(), HttpStatus.valueOf(response.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");
                            return new ResponseEntity<>("Registry unreachable!",
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        log.info("", e);
                        String message = "Registry threw communication exception: " + e.getMessage();
                        log.warn(message);
                        return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("You do not own the Information Model that you tried to delete",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
    }

    public OntologyMapping registerInfoModelMapping(String name, String sourceModelId, String destinationModelId,
                                                    MultipartFile definition, Principal principal)
            throws GenericHttpErrorException {

        log.trace("registerInfoModelMapping");


        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();
        Map<String, String> response = new HashMap<>();

        if (name.length() < 2 || name.length() > 30)
            response.put("info_model_mapping_reg_error_name", "The name should have from 2 to 30 characters");

        if (sourceModelId.equals(destinationModelId))
            response.put("info_model_mapping_reg_error_destination", "The source id is the same with the destination id");

        String definitionString = null;
        try {
            definitionString = new String(definition.getBytes(), StandardCharsets.UTF_8);
            Mapping.parse(definitionString);
            log.debug("The size of the file is " + definition.getBytes().length + "bytes");
        } catch (IOException e) {
            log.warn("The definition could not be parsed as string", e);
            response.put("info_model_mapping_reg_error_definition", e.getMessage());
        } catch (ParseException e) {
            log.warn("The definition of the mapping is not valid", e);
            response.put("info_model_mapping_reg_error_definition", e.getMessage());
        }

        // If there are any errors throw exception
        if (response.size() > 0) {
            response.put("error", "Invalid Arguments");
            throw new GenericBadRequestException("Invalid Arguments", response);
        }

        InfoModelMappingResponse registryResponse;
        try {
            OntologyMapping ontologyMapping = new OntologyMapping();
            ontologyMapping.setName(name);
            ontologyMapping.setOwner(user.getUsername());
            ontologyMapping.setSourceModelId(sourceModelId);
            ontologyMapping.setDestinationModelId(destinationModelId);
            ontologyMapping.setDefinition(definitionString);

            InfoModelMappingRequest request = new InfoModelMappingRequest();
            request.setBody(ontologyMapping);

            registryResponse = rabbitManager.sendRegisterMappingRequest(request);
            if (registryResponse != null) {
                if (registryResponse.getStatus() < 200
                        || registryResponse.getStatus() >= 300) {
                    String message = "Registry responded with: " + registryResponse.getStatus();
                    log.info(message);
                    response.put("error", registryResponse.getMessage());
                    throw new GenericHttpErrorException(message, response,
                            HttpStatus.valueOf(registryResponse.getStatus()));
                }
            } else {
                String message = "Registry unreachable!";
                log.warn(message);
                response.put("error", message);
                throw new GenericInternalServerErrorException(message, response);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message);
            response.put("error", message);
            throw new GenericInternalServerErrorException(message, response);
        }

        return registryResponse.getBody();
    }

    public void deleteInfoModelMapping(String mappingIdToDelete, Principal principal)
        throws GenericHttpErrorException {

        log.trace("deleteInfoModelMapping");

        // Todo: Checking if the user owns the information model mapping
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        // Get mapping info from Registry
        OntologyMapping ontologyMapping = getSingleMapping(new GetSingleMapping(false, mappingIdToDelete));

        if (!ontologyMapping.getOwner().equals(user.getUsername())) {
            log.warn("The user " + user.getUsername() + " does not own the mapping with id " + mappingIdToDelete);
            throw new GenericHttpErrorException("You do not own the mapping with id " + mappingIdToDelete, HttpStatus.UNAUTHORIZED);
        }

        // Ask Registry
        try {
            InfoModelMappingRequest request = new InfoModelMappingRequest();
            request.setBody(ontologyMapping);

            InfoModelMappingResponse response = rabbitManager.sendDeleteMappingRequest(request);
            if (response != null) {
                if (response.getStatus() != HttpStatus.OK.value()) {
                    throw new GenericHttpErrorException(response.getMessage(), HttpStatus.valueOf(response.getStatus()));
                }
            } else {
                log.warn("Registry unreachable!");
                throw new GenericInternalServerErrorException("Registry unreachable!");
            }
        } catch (CommunicationException e) {
            log.info("", e);
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message);
            throw new GenericInternalServerErrorException(message);
        }
    }

    public void getMappingDefinition(String mappingId, HttpServletResponse response)
            throws GenericHttpErrorException {
        // Get mapping info from Registry
        OntologyMapping ontologyMapping = getSingleMapping(new GetSingleMapping(true, mappingId));

        // Create .zip output stream
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
             InputStream stream = new ByteArrayInputStream(ontologyMapping.getDefinition().getBytes(StandardCharsets.UTF_8.name()))) {

            // setting headers
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + mappingId + ".zip\"");
            response.addHeader("Content-Type", "application/zip");

            //packing files
            zipOutputStream.putNextEntry(new ZipEntry(mappingId + ".map"));
            IOUtils.copy(stream, zipOutputStream);
            stream.close();

            zipOutputStream.close();
            response.getOutputStream().close();
        } catch (IOException e) {
            throw new GenericInternalServerErrorException(e.getMessage());
        }
    }

    public ResponseEntity<?> getInformationModels() {
        try {
            InformationModelListResponse informationModelListResponse = rabbitManager.sendListInfoModelsRequest();
            if (informationModelListResponse != null && informationModelListResponse.getStatus() == HttpStatus.OK.value()) {
                return new ResponseEntity<>(informationModelListResponse.getBody(),
                        new HttpHeaders(), HttpStatus.OK);

            } else {
                if (informationModelListResponse != null)
                    return new ResponseEntity<>(informationModelListResponse.getMessage(),
                            new HttpHeaders(), HttpStatus.valueOf(informationModelListResponse.getStatus()));
                else
                    return new ResponseEntity<>("Could not retrieve the information models from registry",
                            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (CommunicationException e) {
            log.info("", e);
            return new ResponseEntity<>("Communication exception while retrieving the information models: " +
                    e.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Set<OntologyMapping> getAllMappings(GetAllMappings getAllMappings)
            throws GenericHttpErrorException {
        MappingListResponse mappingListResponse;
        try {
            mappingListResponse = rabbitManager.sendGetAllMappingsRequest(
                    getAllMappings);
            if (mappingListResponse == null)
                throw new GenericInternalServerErrorException("Registry unreachable!");

            if (mappingListResponse.getStatus() != HttpStatus.OK.value())
                throw new GenericHttpErrorException(mappingListResponse.getMessage(),
                        HttpStatus.valueOf(mappingListResponse.getStatus()));
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            throw new GenericInternalServerErrorException(message);
        }
        return mappingListResponse.getBody();
    }

    private OntologyMapping getSingleMapping(GetSingleMapping getSingleMapping)
            throws GenericHttpErrorException {
        MappingListResponse mappingListResponse;
        try {
            mappingListResponse = rabbitManager.sendGetSingleMappingsRequest(
                    getSingleMapping);
            if (mappingListResponse == null)
                throw new GenericInternalServerErrorException("Registry unreachable!");

            if (mappingListResponse.getStatus() != HttpStatus.OK.value())
                throw new GenericHttpErrorException(mappingListResponse.getMessage(),
                        HttpStatus.valueOf(mappingListResponse.getStatus()));

            if (mappingListResponse.getBody().isEmpty())
                throw new GenericHttpErrorException("Mapping with id " + getSingleMapping.getMappingId() + " was not found",
                        HttpStatus.NOT_FOUND);
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            throw new GenericInternalServerErrorException(message);
        }
        return mappingListResponse.getBody().iterator().next();
    }
}
