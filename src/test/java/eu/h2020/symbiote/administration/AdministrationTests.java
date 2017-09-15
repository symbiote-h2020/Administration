package eu.h2020.symbiote.administration;

import java.util.*;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.core.model.RDFFormat;
import eu.h2020.symbiote.core.model.resources.Resource;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;

import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.communication.payloads.*;

import eu.h2020.symbiote.administration.model.CoreUser;


/**
 * Abstract class with sample objects, acts as parent to all tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
public abstract class AdministrationTests {

    @Value("${aam.deployment.owner.username}")
    private String AAMOwnerUsername;
    @Value("${aam.deployment.owner.password}")
    private String AAMOwnerPassword;


    // ===== Helper Values & Methods ====

    protected String username = "Test1";
    protected String password = "Test1";
    protected String mail = "test@mail.com";

    protected String platformId = "test1Plat";
    protected String name = "Test Platform 1";
    protected String url = "https://platform.test";
    protected String description = "This is a test platform.";

    protected String informationModelId = "model_id";
    protected String informationModelName = "model_name";
    protected String informationModelOwner = "model_owner";
    protected String informationModelUri = "model_uri";
    protected RDFFormat informationModelFormat = RDFFormat.JSONLD;
    protected String informationModelRdf = "model_rdf";

    protected String resourcelId = "resource_id";

    protected String federationRuleId = "federation_rule_id";

    public String serialize(Object o) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }

    public List<GrantedAuthority> sampleAuthorities() {

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
        return grantedAuths;
    }

    public CoreUser sampleCoreUser(UserRole role) throws Exception {


        CoreUser user = new CoreUser(username, password, role, true, true, true, true, sampleAuthorities());
        user.setValidUsername(username);
        user.setValidPassword(password);
        user.setRole(role);
        user.setRecoveryMail(mail);

        return user;
    }

    public Authentication sampleAuth(UserRole role) throws Exception {

        return new UsernamePasswordAuthenticationToken(sampleCoreUser(role), null, sampleAuthorities());
    }

    public Platform sampleEmptyPlatform(){
        

        Platform platform = new Platform();
        platform.setId(platformId);

        return platform;
    }

    public Platform samplePlatform() {

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(informationModelId);
        interworkingService.setUrl(url);

        Platform platform = new Platform();
        platform.setId(platformId);
        platform.setLabels(Arrays.asList(name));
        platform.setComments(Arrays.asList(description));
        platform.setInterworkingServices(Arrays.asList(interworkingService));

        return platform;
    }

    public InformationModel sampleInformationModel() {
        InformationModel model = new InformationModel();
        model.setId(informationModelId);
        model.setUri(informationModelUri);
        model.setOwner(informationModelOwner);
        model.setName(informationModelName);
        model.setRdfFormat(informationModelFormat);
        model.setRdf(informationModelRdf);

        return model;
    }

    public PlatformRegistryResponse samplePlatformResponseSuccess() {

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("Success");
        platformResponse.setBody(samplePlatform());
        return platformResponse;
    }

    public PlatformRegistryResponse samplePlatformResponseFail() {

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(400);
        platformResponse.setMessage("Fail");
        platformResponse.setBody(null);
        return platformResponse;
    }

    public InformationModelListResponse sampleInformationModelListResponseSuccess() {
        InformationModelListResponse response = new InformationModelListResponse();
        List<InformationModel> modelList = new ArrayList<>();
        InformationModel model = sampleInformationModel();
        modelList.add(model);
        response.setBody(modelList);
        response.setStatus(200);
        return response;
    }

    public InformationModelListResponse sampleInformationModelListResponseFail() {
        InformationModelListResponse response = new InformationModelListResponse();
        response.setBody(null);
        response.setMessage("Fail");
        response.setStatus(400);
        return response;
    }

    public InformationModelRequest sampleInformationModelRequest() {
        InformationModelRequest request = new InformationModelRequest();
        request.setBody(sampleInformationModel());
        return request;
    }

    public InformationModelResponse sampleInformationModelResponseSuccess() {
        InformationModelResponse response = new InformationModelResponse();
        response.setStatus(200);
        response.setBody(sampleInformationModel());
        return response;
    }

    public InformationModelResponse sampleInformationModelResponseFail() {
        InformationModelResponse response = new InformationModelResponse();
        response.setBody(null);
        response.setMessage("Fail");
        response.setStatus(400);
        return response;
    }

    public CoreResourceRegistryRequest sampleCoreResourceRegistryRequest() {
        return new CoreResourceRegistryRequest();

    }

    public ResourceListResponse sampleResourceListResponseSuccess() {
        Resource resource = new Resource();
        resource.setId(resourcelId);
        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(resource);

        return new ResourceListResponse(200, "Success!", resourceList);
    }

    public ResourceListResponse sampleResourceListResponseFail() {
        return new ResourceListResponse(400, "Fail!", null);
    }

    public Credentials sampleCredentials() throws Exception {

        CoreUser user = sampleCoreUser(UserRole.PLATFORM_OWNER);
        return new Credentials(user.getValidUsername(), user.getValidPassword());
    }

    public UserManagementRequest sampleUserManagementRequest(UserRole role) throws Exception {

        CoreUser user = sampleCoreUser(role);

        UserManagementRequest request = new UserManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials(username, password),
                new UserDetails(
                    new Credentials(username, password),
                        "",
                        mail,
                        role,
                        new HashMap<>(),
                        new HashMap<>()
                ),
                OperationType.CREATE
            );

        return request;
    }

    public UserDetailsResponse sampleUserDetailsResponse (HttpStatus status) {
        return new UserDetailsResponse(status, new UserDetails(
                new Credentials(username, password),
                "",
                mail,
                UserRole.PLATFORM_OWNER,
                new HashMap<>(),
                new HashMap<>()
        ));
    }

    public PlatformManagementRequest samplePlatformManagementRequest(OperationType operationType) throws Exception {

        PlatformManagementRequest request = new PlatformManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials(username, password),
                url,
                name,
                operationType
            );

        return request;
    }

    public PlatformManagementResponse samplePlatformManagementResponse(ManagementStatus status) throws Exception {

        PlatformManagementResponse response = new PlatformManagementResponse(
                platformId,
                status
            );

        return response;
    }

    public Set<OwnedPlatformDetails> sampleOwnedPlatformDetails() {

        Map<String, Certificate>  componentCerificates = new HashMap<>();
        Set<OwnedPlatformDetails> ownedPlatformDetails = new HashSet<>();
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId, url, name, new Certificate(), componentCerificates));
        return ownedPlatformDetails;
    }

    public FederationRule sampleFederationRule() {
        Set<String> platformIds = new HashSet<>();
        platformIds.add(platformId);
        platformIds.add(platformId);
        return new FederationRule(federationRuleId, platformIds);
    }

    public FederationRuleManagementRequest sampleFederationRuleManagementRequest(
            FederationRuleManagementRequest.OperationType type) throws Exception {
        return new FederationRuleManagementRequest(sampleCredentials(), federationRuleId, type);
    }

    public Map<String, FederationRule> sampleFederationRuleManagementResponse() {
        Map<String, FederationRule> response = new HashMap<>();
        response.put(federationRuleId, sampleFederationRule());

        return response;
    }

    public ErrorResponseContainer sampleErrorResponse() {

        return new ErrorResponseContainer("SAMPLE_ERROR", 400);
    }

    public CommunicationException sampleCommunicationException() {

        return new CommunicationException("SAMPLE_ERROR");
    }
}
