package eu.h2020.symbiote;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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

import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementRequest;
import eu.h2020.symbiote.security.communication.payloads.PlatformManagementResponse;
import eu.h2020.symbiote.security.communication.payloads.UserManagementRequest;
import eu.h2020.symbiote.security.commons.enums.OperationType;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.security.communication.payloads.UserDetails;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;
import eu.h2020.symbiote.security.communication.payloads.ErrorResponseContainer;
import eu.h2020.symbiote.model.CoreUser;
import eu.h2020.symbiote.security.commons.Certificate;



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

    protected String sampleTokenString =
         "eyJhbGciOiJFUzI1NiJ9.eyJTWU1CSU9URV9Pd25lZFBsYXRmb3JtIjoidGVzdDFQbGF0IiwiU1lNQklPVEVfUm9sZSI6IlBMQVR"
        +"GT1JNX09XTkVSIiwidHR5cCI6IkNPUkUiLCJzdWIiOiJUZXN0MSIsImlwayI6Ik1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMER"
        +"BUWNEUWdBRUFSNnUrZk9DNnJLb1grNmFyaWZDSU01Y3Joa3VlOVFsdDZacDVwZE9HemJuZGFUVzJVRzdhY3BnQ3dlNTJhSktZZ1l"
        +"ZZmtIa0JpNCtCOHZDRlhneXp3PT0iLCJpc3MiOiJTeW1iaW90ZSBDb3JlIiwiZXhwIjoxNDk1MDI2ODc2LCJpYXQiOjE0OTUwMjM"
        +"yNzYsImp0aSI6Ii0xNzMwMTg3MTE5Iiwic3BrIjoiTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFb0pNSnpWak1"
        +"RMVJ1U3dIeHJPMGJmaDlhcnFINlBGdUEzOHVBNy9SdXEwdWF2aXFhdE9HWDI2TVU2aW5RcWZaNmtWSThldEpXRmZhU3M3dEh0S2V"
        +"BcVE9PSJ9.fMvSeAo-2FIQUD-TAWqWFbcdP8Bc8TL3Duy28GQ__ckNV7kkDWz_Rf4tGCRC13uyI1Wg7kCNckJoxPckcLoUIg";

    protected String certificate = "sampleCertificate";
    protected String privateKey = "samplePrivateKey";


    public String serialize(Object o) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }

    public Token sampleToken() throws Exception {

        // Map<String, String> claimsAttribute = new HashMap<String, String>();
        // claimsAttribute.put(CoreAttributes.OWNED_PLATFORM.toString(), platformId);

        // JWTClaims claims = new JWTClaims();
        // claims.setAtt(claimsAttribute);

        // Claims claims = new Claims();
        // claims.setId(platformId);

        // Token token = new Token();
        // token.setClaims(claims);

        // return token; 

        return new Token();
    }

    public List<GrantedAuthority> sampleAuthorities(){

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
        return grantedAuths;
    }

    public CoreUser sampleCoreUser() throws Exception {


        CoreUser user = new CoreUser(username, password, true, true, true, true, sampleAuthorities(), sampleToken(), platformId);
        user.setValidUsername(username);
        user.setValidPassword(password);
        user.setFederatedId(federatedId);
        user.setRecoveryMail(mail);

        return user;
    }

    public Authentication sampleAuth() throws Exception {

        return new UsernamePasswordAuthenticationToken(sampleCoreUser(), null, sampleAuthorities());
    }

    public Platform sampleEmptyPlatform(){
        

        Platform platform = new Platform();
        platform.setId(platformId);

        return platform;
    }

    public Platform samplePlatform(){

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

    public PlatformRegistryResponse samplePlatformResponseSuccess(){

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("Success");
        platformResponse.setPlatform(samplePlatform());
        return platformResponse;
    }

    public PlatformRegistryResponse samplePlatformResponseFail(){

        PlatformRegistryResponse platformResponse = new PlatformRegistryResponse();
        platformResponse.setStatus(400);
        platformResponse.setMessage("Fail");
        platformResponse.setPlatform(null);
        return platformResponse;
    }

    public Credentials sampleCredentials() throws Exception {

        CoreUser user = sampleCoreUser();
        return new Credentials(user.getValidUsername(), user.getValidPassword());
    }

    public UserManagementRequest sampleUserManagementRequest() throws Exception {

        CoreUser user = sampleCoreUser();

        UserManagementRequest request = new UserManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials(username, password),
                new UserDetails(
                    new Credentials( username, password),
                    federatedId,
                    mail,
                    UserRole.PLATFORM_OWNER
                ),
                OperationType.CREATE
            );

        return request;
    }

    public PlatformManagementRequest samplePlatformManagementRequest() throws Exception {

        PlatformManagementRequest response = new PlatformManagementRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                new Credentials( username, password),
                url,
                name,
                OperationType.CREATE
            );

        return response;
    }

    public PlatformManagementResponse samplePlatformResponse() throws Exception {

        PlatformManagementResponse response = new PlatformManagementResponse(
                platformId,
                ManagementStatus.OK
            );

        return response;
    }

    public OwnedPlatformDetails sampleOwnerDetails() {

        Map<String, Certificate>  componentCerificates = new HashMap<>();
        return new OwnedPlatformDetails(platformId, url, name, new Certificate(), componentCerificates);
    }

    public ErrorResponseContainer sampleErrorResponse() {

        return new ErrorResponseContainer("SAMPLE_ERROR", 400);
    }
}
