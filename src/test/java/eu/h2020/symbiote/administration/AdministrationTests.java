package eu.h2020.symbiote.administration;

import java.util.*;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
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
    protected String federatedId = "test_fed_id";
    protected String mail = "test@mail.com";

    protected String platformId = "test1Plat";
    protected String name = "Test Platform 1";
    protected String url = "https://platform.test";
    protected String description = "This is a test platform.";
    protected String informationModelId = "test_IM_1";


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

    public ErrorResponseContainer sampleErrorResponse() {

        return new ErrorResponseContainer("SAMPLE_ERROR", 400);
    }

    public CommunicationException sampleCommunicationException() {

        return new CommunicationException("SAMPLE_ERROR");
    }
}
