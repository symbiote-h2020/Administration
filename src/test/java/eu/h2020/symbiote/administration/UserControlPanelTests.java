package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.controllers.UserCpanel;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.administration.model.Description;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.model.PlatformDetails;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class UserControlPanelTests extends AdministrationTests {
    private static Log log = LogFactory.getLog(AdministrationTests.class);

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Mock
    private RabbitManager mockRabbitManager;

    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);

        CustomAuthenticationProvider provider = appContext.getBean(CustomAuthenticationProvider.class);
        provider.setRabbitManager(mockRabbitManager);

        Register registerController = appContext.getBean(Register.class);
        registerController.setRabbitManager(mockRabbitManager);

        UserCpanel userCpanelController = appContext.getBean(UserCpanel.class);
        userCpanelController.setRabbitManager(mockRabbitManager);
    }



    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/administration/user/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/administration/user/login"));
    }

    @Test
    public void getControlPanelSuccess() throws Exception {

        mockMvc.perform(get("/administration/user/cpanel")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER))) )
            .andExpect(status().isOk());

    }


    @Test
    public void getListUserPlatforms() throws Exception {

        // Get all platform information from Registry
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(platformId);

        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All the owned platform details were successfully received"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId));


        // Get all partial platform information from Registry
        Set<OwnedPlatformDetails> ownedPlatformDetailsSet = new HashSet<>();
        ownedPlatformDetailsSet.add(new OwnedPlatformDetails(platformId, platformUrl, platformName, new Certificate(), new HashMap<>()));
        ownedPlatformDetailsSet.add(new OwnedPlatformDetails(platformId + "2", platformUrl, platformName + "2",
                new Certificate(), new HashMap<>()));

        doReturn(ownedPlatformDetailsSet).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(eq(platformId + "2"));


        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isPartialContent())
                .andExpect(jsonPath("$.message")
                        .value("Could NOT retrieve information from Registry for the following platform that you own: "
                                + platformName + "2"))
                .andExpect(jsonPath("$.availablePlatforms.length()").value(1))
                .andExpect(jsonPath("$.availablePlatforms[0].id").value(platformId));


        // Registry unreachable
        doReturn(null).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry unreachable!"));


        // Registry threw communication exception
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry threw CommunicationException: error"));


        // AAM responds with null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM responded with null"));


        // AAM threw communication exception
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/administration/user/cpanel/list_user_platforms")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM threw CommunicationException: error"));

    }


    @Test
    public void registerPlatform() throws Exception {

        // Could not get Information modes from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));


        // Register platform successfully
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform-registration-success")
                        .value("Successful Registration!"));


        // Registry responds with error
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value(samplePlatformResponseFail().getMessage()));


        // Registry responds with null
        doReturn(null).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry unreachable!"));


        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry threw CommunicationException"));


        // AAM responds with PLATFORM_EXISTS
        doReturn(samplePlatformManagementResponse(ManagementStatus.PLATFORM_EXISTS)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that the Platform exists!"));


        // AAM responds with other ERROR
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that there was an ERROR"));


        // AAM responds with null
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM unreachable!"));


        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM threw CommunicationException: error"));

        // Invalid Arguments Check
        InformationModelListResponse informationModelListResponse = sampleInformationModelListResponseSuccess();
        informationModelListResponse.getBody().get(0).setId("dummy");

        PlatformDetails platformDetails = samplePlatformDetails();
        platformDetails.setName("aa");
        platformDetails.getDescription().add(new Description("aa"));
        platformDetails.getDescription().add(new Description("aaaa"));
        platformDetails.getDescription().add(new Description("aa"));

        doReturn(informationModelListResponse).when(mockRabbitManager)
                .sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(platformDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.pl_reg_error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.pl_reg_error_description_description.length()")
                        .value(4))
                .andExpect(jsonPath("$.pl_reg_error_description_description[1]")
                        .value("Length must be between 4 and 300 characters"))
                .andExpect(jsonPath("$.pl_reg_error_description_description[3]")
                        .value("Length must be between 4 and 300 characters"));

    }


    @Test
    public void deletePlatforms() throws Exception {

        // Delete Platform Successfully
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isOk());

        // The user does not own the platform which tries to delete
        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));

        // AAM return null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable"));

        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));


        // Registry returns error
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(samplePlatformResponseFail().getMessage()));

        // Registry returns null
        doReturn(null).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));

        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));


        // AAM returns error
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("AAM says that the Platform does not exist!"));

        // AAM returns null
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable!"));

        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));
    }


    @Test
    public void getPlatformConfig() throws Exception {

        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        // User does not own the platform
        PlatformConfigurationMessage invalidPlatform = samplePlatformConfigurationMessage();
        invalidPlatform.setPlatformId("dummy");

        mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invalidPlatform)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));

        // Successful Request
        MvcResult mvcResult = mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformConfigurationMessage())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"configuration.zip\""))
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        Map<String, String> zipFiles = new HashMap<>();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray()));

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zis, baos);
            zipFiles.put(fileName, baos.toString(StandardCharsets.UTF_8.name()));
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        String cloudCoreInterfaceAddress = this.coreInterfaceAddress.replace("8100/coreInterface", "8101/cloudCoreInterface");

        // Checking application.properties of CloudConfigProperties
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.host=localhost"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.username=guest"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.password=guest"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.core.interface.url="
                + this.coreInterfaceAddress));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.core.cloud.interface.url="
                + cloudCoreInterfaceAddress));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.interworking.interface.url="
                + platformUrl + "/cloudCoreInterface/v1"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.localaam.url="
                + platformUrl + "/paam"));

        // Checking nginx.conf
        assertTrue(zipFiles.get("nginx.conf").contains("proxy_pass  " + coreInterfaceAddress + "/;"));
        assertTrue(zipFiles.get("nginx.conf").contains("proxy_pass  " + cloudCoreInterfaceAddress + "/;"));

        // Checking bootstrap.properties of Registration Handler
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.interworking.interface.url=" + platformUrl));

        // Checking bootstrap.properties of RAP
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbiote.notification.url=" + cloudCoreInterfaceAddress + "/accessNotifications"));

        // Checking bootstrap.properties of Monitoring
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.interworking.interface.url=" + platformUrl));

        // Checking bootstrap.properties of AAM
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.owner.username=" + username));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.owner.password=" + password));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.KEY_STORE_FILE_NAME=" + aamKeystoreName + ".p12"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.ROOT_CA_CERTIFICATE_ALIAS=caam"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.CERTIFICATE_ALIAS=paam"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.KEY_STORE_PASSWORD=" + aamKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.PV_KEY_PASSWORD=" + aamKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.token.validityMillis=" + tokenValidity));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-store=classpath:" + sslKeystore));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-store-password=" + sslKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-password=" + sslKeyPassword));

        // Checking PlatformAAMCertificateKeyStoreFactory
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String coreAAMAddress = \"" + coreInterfaceAddress + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformOwnerUsername = \"" + username + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformOwnerPassword = \"" + password + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformId = \"" + platformId + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String keyStoreFileName = \"" + aamKeystoreName + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String keyStorePassword = \"" + aamKeystorePassword + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String aamCertificateAlias = \"paam\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String rootCACertificateAlias = \"caam\";"));

    }


    @Test
    public void listAllInformationModels() throws Exception {
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_all_info_models")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleInformationModel().getId()));
    }

    @Test
    public void listUserInformationModels() throws Exception {
        // Successfully listing user's information model
        InformationModel infoModel = sampleInformationModel();
        infoModel.setOwner("dummy");

        InformationModelListResponse informationModelListResponse = sampleInformationModelListResponseSuccess();
        informationModelListResponse.getBody().add(infoModel);

        doReturn(informationModelListResponse).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleInformationModel().getId()));

        // Registry returns error
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/list_user_info_models")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }


    @Test
    public void registerInformationModel() throws Exception {
        // Validate information model details
        MockMultipartFile invalidFile = new MockMultipartFile("info-model-rdf", "mock.ff",
                "text/plain", "dummy content".getBytes());

        mockMvc.perform(fileUpload("/administration/user/cpanel/administration/register_information_model")
                .file(invalidFile)
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", "a")
                .param("info-model-uri", "url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info_model_reg_error_name").value("The name should have from 2 to 30 characters"))
                .andExpect(jsonPath("$.info_model_reg_error_uri").value("The uri is invalid"))
                .andExpect(jsonPath("$.info_model_reg_error_rdf").value("This format is not supported"))
                .andExpect(jsonPath("$.error").value("Invalid Arguments"));


        // Successful information model registration
        doReturn(sampleInformationModelResponseSuccess()).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        MockMultipartFile validFile = new MockMultipartFile("info-model-rdf", "mock.ttl",
                "text/plain", informationModelRdf.getBytes());

        mockMvc.perform(fileUpload("/administration/user/cpanel/administration/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(status().isCreated());

        // Registry returns error
        doReturn(sampleInformationModelResponseFail()).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/administration/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value(sampleInformationModelResponseFail().getMessage()));

        // Registry returns null
        doReturn(null).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/administration/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry unreachable!"));

        // Registry returns null
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/administration/user/cpanel/administration/register_information_model")
                .file(validFile)
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry threw communication exception: error"));

    }

    @Test
    public void deleteInformationModel() throws Exception {

        // Could not get Information modes from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));


        // The user does not own the information model which tried to delete
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", "dummyid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the Information Model that you tried to delete"));


        // Delete information model successfully
        doReturn(sampleInformationModelResponseSuccess()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isOk());


        // Registry returns error
        doReturn(sampleInformationModelResponseFail()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(sampleInformationModelResponseFail().getMessage()));


        // Registry returns null
        doReturn(null).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));


        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/delete_information_model")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));
    }


    @Test
    public void getInformationModels() throws Exception {

        // Failed response
        doReturn(sampleInformationModelListResponseFail()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(sampleInformationModelListResponseFail().getMessage()));


        // Registry returns null
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));

        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/administration/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Communication exception while retrieving the information models: error"));
    }

}