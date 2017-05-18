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


    // ===== Helper Methods ====

    public Token sampleToken() throws Exception {

        String tokenString = "eyJhbGciOiJFUzI1NiJ9.eyJTWU1CSU9URV9Pd25lZFBsYXRmb3JtIjoidGVzdDFQbGF0IiwiU1lNQklPVEVfUm9sZSI6IlBMQVR"
                            +"GT1JNX09XTkVSIiwidHR5cCI6IkNPUkUiLCJzdWIiOiJUZXN0MSIsImlwayI6Ik1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMER"
                            +"BUWNEUWdBRUFSNnUrZk9DNnJLb1grNmFyaWZDSU01Y3Joa3VlOVFsdDZacDVwZE9HemJuZGFUVzJVRzdhY3BnQ3dlNTJhSktZZ1l"
                            +"ZZmtIa0JpNCtCOHZDRlhneXp3PT0iLCJpc3MiOiJTeW1iaW90ZSBDb3JlIiwiZXhwIjoxNDk1MDI2ODc2LCJpYXQiOjE0OTUwMjM"
                            +"yNzYsImp0aSI6Ii0xNzMwMTg3MTE5Iiwic3BrIjoiTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFb0pNSnpWak1"
                            +"RMVJ1U3dIeHJPMGJmaDlhcnFINlBGdUEzOHVBNy9SdXEwdWF2aXFhdE9HWDI2TVU2aW5RcWZaNmtWSThldEpXRmZhU3M3dEh0S2V"
                            +"BcVE9PSJ9.fMvSeAo-2FIQUD-TAWqWFbcdP8Bc8TL3Duy28GQ__ckNV7kkDWz_Rf4tGCRC13uyI1Wg7kCNckJoxPckcLoUIg";
        return new Token(tokenString);
    }

    public List<GrantedAuthority> sampleAuths(){

        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
        return grantedAuths;
    }

    public CoreUser sampleCoreUser() throws Exception {

        String username = "Test1";
        String password = "Test1";
        String platformId = "test1Plat";
        String federatedId = "test_fed_id";
        String mail = "test@mail.com";

        CoreUser user = new CoreUser(username, password, true, true, true, true, sampleAuths(), sampleToken(), platformId);
        user.setValidUsername(username);
        user.setValidPassword(password);
        user.setFederatedId(federatedId);
        user.setRecoveryMail(mail);

        return user;
    }

    public Platform sampleEmptyPlatform(){
        
        String platformId = "test1Plat";

        Platform platform = new Platform();
        platform.setPlatformId(platformId);

        return platform;
    }

    public Platform samplePlatform(){

        String platformId = "test1Plat";
        String name = "Test Platform 1";
        String url = "https://platform.test:8101";
        String description = "This is a test platform.";
        String informationModelId = "test_IM_1";

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

    public PlatformRegistrationRequest samplePlatformRequest() throws Exception {

        CoreUser user = sampleCoreUser();

        UserDetails userDetails = new UserDetails(
                new Credentials( user.getValidUsername(), user.getValidPassword()),
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

        String certificate = "sampleCertificate";
        String privateKey = "samplePrivateKey";
        String platformId = "test1Plat";

        PlatformRegistrationResponse response = new PlatformRegistrationResponse(
                new Certificate(certificate),
                privateKey,
                platformId
            );

        return response;
    }
}
