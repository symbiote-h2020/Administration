package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.Description;
import eu.h2020.symbiote.administration.model.PlatformDetails;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.core.internal.InformationModelListResponse;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterPlatformTests extends UserControlPanelBaseTestClass {

    @Test
    public void informationModelsError() throws Exception {
        // Could not get Information models from Registry
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }

    @Test
    public void success() throws Exception {
        // Register platform successfully
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value(platformName));
    }

    @Test
    public void registryError() throws Exception {
        // Registry responds with error
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value(samplePlatformResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry responds with null
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(null).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformCreationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("Registry threw CommunicationException"));
    }

    @Test
    public void platformExists() throws Exception {
        // AAM responds with PLATFORM_EXISTS
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.PLATFORM_EXISTS)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that the Platform exists!"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM responds with other ERROR
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM says that there was an ERROR"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformRegistrationError")
                        .value("AAM threw CommunicationException: error"));
    }

    @Test
    public void invalidArguments() throws Exception {
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

        mockMvc.perform(post("/administration/user/cpanel/register_platform")
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
}