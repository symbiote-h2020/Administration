package eu.h2020.symbiote;

import java.util.List;
import java.util.ArrayList;
import javax.servlet.Filter;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationRequest;
import eu.h2020.symbiote.security.payloads.PlatformRegistrationResponse;
import eu.h2020.symbiote.security.payloads.Credentials;
import eu.h2020.symbiote.security.payloads.UserDetails;
import eu.h2020.symbiote.security.enums.UserRole;
import eu.h2020.symbiote.model.CoreUser;
import eu.h2020.symbiote.security.certificate.Certificate;
import eu.h2020.symbiote.security.payloads.OwnedPlatformDetails;
import eu.h2020.symbiote.security.payloads.ErrorResponseContainer;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
public abstract class AdministrationTests {

    @Value("${aam.deployment.owner.username}")
    private String AAMOwnerUsername;
    @Value("${aam.deployment.owner.password}")
    private String AAMOwnerPassword;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    protected MockMvc mockMvc;

    @Before
    public void setup(){

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();
    }


    // ===== Helper Values & Methods ====

    protected String username = "Test1";
    protected String password = "Test1";
    protected String federatedId = "test_fed_id";
    protected String mail = "test@mail.com";

    protected String platformId = "test1Plat";
    protected String name = "Test Platform 1";
    protected String url = "https://platform.test:8101";
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

        return new Token(sampleTokenString); 
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
        platform.setPlatformId(platformId);

        return platform;
    }

    public Platform samplePlatform(){

        Platform platform = new Platform();
        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setUrl(url);
        platform.setDescription(description);
        platform.setInformationModelId(informationModelId);

        return platform;
    }

    public PlatformResponse samplePlatformResponseSuccess(){

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("Success");
        platformResponse.setPlatform(samplePlatform());
        return platformResponse;
    }

    public PlatformResponse samplePlatformResponseFail(){

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(400);
        platformResponse.setMessage("Fail");
        platformResponse.setPlatform(null);
        return platformResponse;
    }

    public Credentials sampleCredentials() throws Exception {

        CoreUser user = sampleCoreUser();
        return new Credentials(user.getValidUsername(), user.getValidPassword());
    }

    public PlatformRegistrationRequest samplePlatformRequest() throws Exception {

        CoreUser user = sampleCoreUser();

        UserDetails userDetails = new UserDetails(
                sampleCredentials(),
                user.getFederatedId(),
                user.getRecoveryMail(),
                UserRole.PLATFORM_OWNER
            );

        PlatformRegistrationRequest request = new PlatformRegistrationRequest(
                new Credentials(AAMOwnerUsername, AAMOwnerPassword),
                userDetails,
                user.getPlatformUrl(),
                user.getPlatformName(),
                user.getPlatformId()
            );

        return request;
    }

    public PlatformRegistrationResponse samplePlatformResponse() throws Exception {

        PlatformRegistrationResponse response = new PlatformRegistrationResponse(
                new Certificate(certificate),
                privateKey,
                platformId
            );

        return response;
    }

    public OwnedPlatformDetails sampleOwnerDetails() {

        return new OwnedPlatformDetails(platformId, url, name);
    }

    public ErrorResponseContainer sampleErrorResponse() {

        return new ErrorResponseContainer("SAMPLE_ERROR", 400);
    }
}
