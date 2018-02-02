package eu.h2020.symbiote.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.RDFFormat;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;


/**
 * Abstract class with sample objects, acts as parent to all tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
public abstract class AdministrationBaseTestClass {

    @Value("${aam.deployment.owner.username}")
    protected String AAMOwnerUsername;
    @Value("${aam.deployment.owner.password}")
    protected String AAMOwnerPassword;
    @Value("${paam.deployment.token.validityMillis}")
    protected String paamValidityMillis;
    @Value("${aam.environment.coreInterfaceAddress}")
    protected String coreInterfaceAddress;

    // ===== Helper Values & Methods ====

    protected String username = "Test1";
    protected String password = "Test1$";
    protected String mail = "test@mail.com";

    protected String platformId = "test1Plat";
    protected String platformPort = "8102";
    protected String platformName = "Test Platform 1";
    protected String platformUrl = "https://platform.test:" + platformPort + "/paam";
    protected String platformDescription = "This is a test platform.";

    protected String informationModelId = "model_id";
    protected String informationModelName = "model_name";
    protected String informationModelOwner = username;
    protected String informationModelUri = "http://model-uri.com";
    protected RDFFormat informationModelFormat = RDFFormat.JSONLD;
    protected String informationModelRdf = "model_rdf";

    protected String resourcelId = "resource_id";

    protected String federationRuleId = "federation_rule_id";

    protected String componentsKeystorePassword = "comp_pass";
    protected String aamKeystoreName = "keystore";
    protected String aamKeystorePassword = "aampass";
    protected String aamPrivateKeyPassword = "private_key_pass";
    protected String sslKeystore = "ssl_keystore";
    protected String sslKeystorePassword = "ssl_keystore_pass";
    protected String sslKeyPassword = "ssl_key_pass";
    protected Long tokenValidity = 100L;
    protected Boolean useBuiltInRapPlugin = true;

    public String serialize(Object o) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }

    public List<GrantedAuthority> sampleUserAuthorities() {

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
        return grantedAuths;
    }

    public List<GrantedAuthority> sampleAdminAuthorities() {

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return grantedAuths;
    }

    public CoreUser sampleCoreUser(UserRole role) {


        CoreUser user = new CoreUser(username, password, role, true, true,
                true, true, sampleUserAuthorities());
        user.setValidUsername(username);
        user.setValidPassword(password);
        user.setRole(role);
        user.setRecoveryMail(mail);

        return user;
    }

    public CoreUser sampleAdminUser(UserRole role) {


        CoreUser user = new CoreUser(username, password, role, true, true,
                true, true, sampleAdminAuthorities());
        user.setValidUsername(username);
        user.setValidPassword(password);
        user.setRole(role);
        user.setRecoveryMail(mail);

        return user;
    }

    public Authentication sampleUserAuth(UserRole role) {

        return new UsernamePasswordAuthenticationToken(sampleCoreUser(role), password, sampleUserAuthorities());
    }

    public Authentication sampleAdminAuth(UserRole role) {

        return new UsernamePasswordAuthenticationToken(sampleAdminUser(role), null, sampleAdminAuthorities());
    }

    public Platform sampleEmptyPlatform(){
        

        Platform platform = new Platform();
        platform.setId(platformId);

        return platform;
    }

    public ChangePasswordRequest sampleChangePasswordRequest() {
        return new ChangePasswordRequest(password,"newPassword", "newPassword");
    }

    public ChangeEmailRequest sampleChangeEmailRequest() {
        return new ChangeEmailRequest("new@email.com", "new@email.com");
    }

    public Platform samplePlatform() {

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(informationModelId);
        interworkingService.setUrl(platformUrl);

        Platform platform = new Platform();
        platform.setId(platformId);
        platform.setName(platformName);
        platform.setDescription(Collections.singletonList(platformDescription));
        platform.setInterworkingServices(Collections.singletonList(interworkingService));

        return platform;
    }

    public PlatformDetails samplePlatformDetails() {

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(informationModelId);
        interworkingService.setUrl(platformUrl);

        List<Description> descriptions = new ArrayList<>();
        descriptions.add(new Description(platformDescription));

        PlatformDetails platformDetails = new PlatformDetails();
        platformDetails.setId(platformId);
        platformDetails.setInterworkingServices(Collections.singletonList(interworkingService));
        platformDetails.setName(platformName);
        platformDetails.setDescription(descriptions);
        platformDetails.setIsEnabler(false);

        return platformDetails;
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

    public PlatformConfigurationMessage samplePlatformConfigurationMessage() {

        return new PlatformConfigurationMessage(platformId, username, password, componentsKeystorePassword,
                aamKeystoreName, aamKeystorePassword, aamPrivateKeyPassword, sslKeystore,
                sslKeystorePassword, sslKeyPassword, tokenValidity, useBuiltInRapPlugin);
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

    public Credentials sampleCredentials() {

        CoreUser user = sampleCoreUser(UserRole.PLATFORM_OWNER);
        return new Credentials(user.getValidUsername(), user.getValidPassword());
    }

    public UserManagementRequest sampleUserManagementRequest(UserRole role) {

        return new UserManagementRequest(
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

    public PlatformManagementRequest samplePlatformManagementRequest(OperationType operationType) {

        return new PlatformManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials(username, password),
                platformUrl,
                platformName,
                operationType
            );
    }

    public PlatformManagementResponse samplePlatformManagementResponse(ManagementStatus status) {

        return new PlatformManagementResponse(
                platformId,
                status
            );
    }

    public Set<OwnedPlatformDetails> sampleOwnedPlatformDetails() {

        Map<String, Certificate>  componentCertificates = new HashMap<>();
        Set<OwnedPlatformDetails> ownedPlatformDetails = new HashSet<>();
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId, platformUrl, platformName, new Certificate(), componentCertificates));
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId + '2', platformUrl, platformName, new Certificate(), componentCertificates));
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId + '3', platformUrl, platformName, new Certificate(), componentCertificates));
        ownedPlatformDetails.add(new OwnedPlatformDetails(platformId + '4', platformUrl, platformName, new Certificate(), componentCertificates));
        return ownedPlatformDetails;
    }

    public CreateFederationRequest sampleCreateFederationRequest() {
        ArrayList<PlatformId> platformIds = new ArrayList<>();
        platformIds.add(new PlatformId(platformId));
        platformIds.add(new PlatformId(platformId + '2'));
        platformIds.add(new PlatformId(platformId + '3'));
        return new CreateFederationRequest(federationRuleId, platformIds);
    }

    public FederationRule sampleFederationRule() {
        Set<String> platformIds = new HashSet<>();
        platformIds.add(platformId);
        platformIds.add(platformId + '2');
        platformIds.add(platformId + '3');
        return new FederationRule(federationRuleId, platformIds);
    }

    public FederationRuleManagementRequest sampleFederationRuleManagementRequest(
            FederationRuleManagementRequest.OperationType type) {
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
