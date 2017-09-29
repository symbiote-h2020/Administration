package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.controllers.Cpanel;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.administration.model.Comment;
import eu.h2020.symbiote.administration.model.PlatformDetails;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.core.model.InformationModel;
import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
public class ControlPanelTests extends AdministrationTests {

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

        Cpanel cpanelController = appContext.getBean(Cpanel.class);
        cpanelController.setRabbitManager(mockRabbitManager);
    }



    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/user/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/user/login"));
    }

    @Test
    public void getControlPanelSuccess() throws Exception {

        mockMvc.perform(get("/user/cpanel")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER))) )
            .andExpect(status().isOk());

    }


    @Test
    public void getListUserPlatforms() throws Exception {

        // Get all platform information from Registry
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(platformId);

        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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


        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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
        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry unreachable!"));


        // Registry threw communication exception
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendGetPlatformDetailsMessage(any());
        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Registry threw CommunicationException: error"));


        // AAM responds with null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM responded with null"));


        // AAM threw communication exception
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        mockMvc.perform(post("/user/cpanel/list_user_platforms")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("AAM threw CommunicationException: error"));

    }


    @Test
    public void registerPlatform() throws Exception {

        // Could not get Information modes from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform-registration-success")
                        .value("Successful Registration!"));


        // Registry responds with error
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value(samplePlatformResponseFail().getMessage()));


        // Registry responds with null
        doReturn(null).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry unreachable!"));


        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry threw CommunicationException"));


        // AAM responds with PLATFORM_EXISTS
        doReturn(samplePlatformManagementResponse(ManagementStatus.PLATFORM_EXISTS)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that the Platform exists!"));


        // AAM responds with other ERROR
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that there was an ERROR"));


        // AAM responds with null
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM unreachable!"));


        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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
        platformDetails.getComments().add(new Comment("aa"));
        platformDetails.getComments().add(new Comment("aaaa"));
        platformDetails.getComments().add(new Comment("aa"));

        doReturn(informationModelListResponse).when(mockRabbitManager)
                .sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/register_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(platformDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.pl_reg_error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.pl_reg_error_comments_comment.length()")
                        .value(4))
                .andExpect(jsonPath("$.pl_reg_error_comments_comment[1]")
                        .value("Length must be between 4 and 300 characters"))
                .andExpect(jsonPath("$.pl_reg_error_comments_comment[3]")
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

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isOk());

        // The user does not own the platform which tries to delete
        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform that you tried to delete"));

        // AAM return null
        doReturn(null).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable"));

        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));


        // Registry returns error
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(samplePlatformResponseFail().getMessage()));

        // Registry returns null
        doReturn(null).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));

        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));


        // AAM returns error
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformRemovalRequest(any());
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("AAM says that the Platform does not exist!"));

        // AAM returns null
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM unreachable!"));

        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_platform")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("platformIdToDelete", platformId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("AAM threw communication exception: error"));
    }


    @Test
    public void listAllInformationModels() throws Exception {
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/list_all_info_models")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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

        mockMvc.perform(post("/user/cpanel/list_user_info_models")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value(sampleInformationModel().getId()));

        // Registry returns error
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/list_user_info_models")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }


    @Test
    public void registerInformationModel() throws Exception {
        // Validate information model details
        MockMultipartFile invalidFile = new MockMultipartFile("info-model-rdf", "mock.ff",
                "text/plain", "dummy content".getBytes());

        mockMvc.perform(fileUpload("/user/cpanel/register_information_model")
                .file(invalidFile)
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
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

        mockMvc.perform(fileUpload("/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(status().isCreated());

        // Registry returns error
        doReturn(sampleInformationModelResponseFail()).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value(sampleInformationModelResponseFail().getMessage()));

        // Registry returns null
        doReturn(null).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry unreachable!"));

        // Registry returns null
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendRegisterInfoModelRequest(any());

        mockMvc.perform(fileUpload("/user/cpanel/register_information_model")
                .file(validFile)
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("info-model-name", informationModelName)
                .param("info-model-uri", informationModelUri))
                .andExpect(jsonPath("$.error").value("Registry threw communication exception: error"));

    }

    @Test
    public void deleteInformationModel() throws Exception {

        // Could not get Information modes from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));


        // The user does not own the information model which tried to delete
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", "dummyid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the Information Model that you tried to delete"));


        // Delete information model successfully
        doReturn(sampleInformationModelResponseSuccess()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isOk());


        // Registry returns error
        doReturn(sampleInformationModelResponseFail()).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(sampleInformationModelResponseFail().getMessage()));


        // Registry returns null
        doReturn(null).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry unreachable!"));


        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendDeleteInfoModelRequest(any());

        mockMvc.perform(post("/user/cpanel/delete_information_model")
                .with(authentication(sampleAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("infoModelIdToDelete", informationModelId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Registry threw communication exception: error"));
    }

}