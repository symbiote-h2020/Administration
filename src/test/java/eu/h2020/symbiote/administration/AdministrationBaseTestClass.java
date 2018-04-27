package eu.h2020.symbiote.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.*;
import eu.h2020.symbiote.administration.repository.FederationRepository;
import eu.h2020.symbiote.administration.services.AuthorizationService;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.cci.PlatformRegistryResponse;
import eu.h2020.symbiote.core.internal.CoreResourceRegistryRequest;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.internal.RDFFormat;
import eu.h2020.symbiote.core.internal.ResourceListResponse;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.mim.Comparator;
import eu.h2020.symbiote.model.mim.*;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.*;
import eu.h2020.symbiote.security.communication.payloads.OwnedService.ServiceType;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.*;


/**
 * Abstract class with sample objects, acts as parent to all tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class AdministrationBaseTestClass {

    @Autowired
    protected FederationRepository federationRepository;

    @Autowired
    protected AuthorizationService authorizationService;

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected RabbitManager rabbitManager;

    protected ClientHttpRequestFactory originalRequestFactory;

    @Autowired
    @InjectMocks
    protected CustomAuthenticationProvider provider;

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

    protected String sspId = "testSSP";
    protected String sspName = "testSSPName";
    protected String sspExternalAddress = "https://www.external.com";
    protected String sspSiteLocalAddress = "https://www.local.com";
    protected boolean exposingSSPSiteLocalAddress = true;

    protected String informationModelId = "model_id";
    protected String informationModelName = "model_name";
    protected String informationModelOwner = username;
    protected String informationModelUri = "http://model-uri.com";
    protected RDFFormat informationModelFormat = RDFFormat.JSONLD;
    protected String informationModelRdf = "model_rdf";

    protected String resourcelId = "resource_id";

    protected String federationId = "federationId";
    protected String federationName = "federationName";

    protected String componentsKeystorePassword = "comp_pass";
    protected String aamKeystoreName = "keystore";
    protected String aamKeystorePassword = "aampass";
    protected String aamPrivateKeyPassword = "private_key_pass";
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
        return samplePlatform(platformId);
    }

    public Platform samplePlatform(String platformId) {

        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setInformationModelId(informationModelId);
        interworkingService.setUrl(platformUrl + "/" + platformId);

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

    public SSPDetails sampleSSPDetails() {
        return new SSPDetails(sspId, sspName, sspExternalAddress, sspSiteLocalAddress, exposingSSPSiteLocalAddress);
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

    public PlatformRegistryResponse samplePlatformResponseSuccess(String platformId) {

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("Success");
        platformResponse.setBody(samplePlatform(platformId));
        return platformResponse;
    }

    public PlatformRegistryResponse samplePlatformResponseFail() {

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(400);
        platformResponse.setMessage("Fail");
        platformResponse.setBody(null);
        return platformResponse;
    }

    public PlatformConfigurationMessage samplePlatformConfigurationMessage(PlatformConfigurationMessage.Level level) {

        return new PlatformConfigurationMessage(platformId, username, password, componentsKeystorePassword,
                aamKeystoreName, aamKeystorePassword, aamPrivateKeyPassword, tokenValidity, useBuiltInRapPlugin, level);
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

        CoreUser user = sampleCoreUser(UserRole.SERVICE_OWNER);
        return new Credentials(user.getValidUsername(), user.getValidPassword());
    }

    public UserManagementRequest sampleUserManagementRequest(UserRole role) {

        return new UserManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials(username, password),
                new UserDetails(
                    new Credentials(username, password),
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
                mail,
                UserRole.SERVICE_OWNER,
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

    public SmartSpaceManagementResponse sampleSmartSpaceManagementResponse(ManagementStatus status) {

        return new SmartSpaceManagementResponse(
                sspId,
                status
        );
    }

    public Set<OwnedService> sampleOwnedServiceDetails() {

        Map<String, Certificate>  componentCertificates = new HashMap<>();
        Set<OwnedService> ownedServiceSet = new HashSet<>();

        // Platforms
        ownedServiceSet.add(new OwnedService(
                platformId, platformName, ServiceType.PLATFORM, platformUrl, null,
                false, null, new Certificate(), componentCertificates));
        ownedServiceSet.add(new OwnedService(
                platformId + "2", platformName, ServiceType.PLATFORM, platformUrl, null,
                false, null, new Certificate(), componentCertificates));
        ownedServiceSet.add(new OwnedService(
                platformId + "3", platformName, ServiceType.PLATFORM, platformUrl, null,
                false, null, new Certificate(), componentCertificates));
        ownedServiceSet.add(new OwnedService(
                platformId + "4", platformName, ServiceType.PLATFORM, platformUrl, null,
                false, null, new Certificate(), componentCertificates));

        // SSPs
        ownedServiceSet.add(new OwnedService(
                sspId, platformName, ServiceType.SMART_SPACE, null, sspExternalAddress,
                false, sspSiteLocalAddress, new Certificate(), componentCertificates));

        ownedServiceSet.add(new OwnedService(
                sspId + "2", platformName, ServiceType.SMART_SPACE, null, sspExternalAddress,
                true, sspSiteLocalAddress, new Certificate(), componentCertificates));

        return ownedServiceSet;
    }

    public Federation sampleFederationRequest() {
        Federation federation = new Federation();
        federation.setId(federationId);
        federation.setInformationModel(sampleInformationModel());
        federation.setName(federationName);
        federation.setPublic(true);


        QoSConstraint qosConstraint1 = new QoSConstraint();
        qosConstraint1.setMetric(QoSMetric.availability);
        qosConstraint1.setComparator(Comparator.equal);
        qosConstraint1.setThreshold(1.2);
        QoSConstraint qosConstraint2 = new QoSConstraint();
        qosConstraint2.setMetric(QoSMetric.load);
        qosConstraint2.setComparator(Comparator.greaterThan);
        qosConstraint2.setThreshold(1.2);
        federation.setSlaConstraints(new ArrayList<>(Arrays.asList(qosConstraint1, qosConstraint2)));

        List<QoSConstraint> qosConstraints = new ArrayList<>(Arrays.asList(qosConstraint1, qosConstraint2));
        federation.setSlaConstraints(qosConstraints);
        FederationMember member1 = new FederationMember(platformId, null);
        FederationMember member2 = new FederationMember(platformId + '2', null);
        FederationMember member3 = new FederationMember(platformId + '3', null);
        federation.setMembers(new ArrayList<>(Arrays.asList(member1, member2, member3)));

        return federation;
    }

    public Federation sampleSavedFederation() {
        Federation federation = sampleFederationRequest();

        String platform1Url = platformUrl + "/" + platformId;
        String platformId2 = platformId + "2";
        String platform2Url = platformUrl + "/" + platformId2;
        String platformId3 = platformId + "3";
        String platform3Url = platformUrl + "/" + platformId3;

        federation.getMembers().get(0).setInterworkingServiceURL(platform1Url);
        federation.getMembers().get(1).setInterworkingServiceURL(platform2Url);
        federation.getMembers().get(2).setInterworkingServiceURL(platform3Url);

        return federation;
    }

    public ErrorResponseContainer sampleErrorResponse() {

        return new ErrorResponseContainer("SAMPLE_ERROR", 400);
    }

    public CommunicationException sampleCommunicationException() {

        return new CommunicationException("SAMPLE_ERROR");
    }
}
