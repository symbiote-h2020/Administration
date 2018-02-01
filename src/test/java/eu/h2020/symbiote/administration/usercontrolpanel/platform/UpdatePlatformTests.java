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
public class UpdatePlatformTests extends UserControlPanelBaseTestClass {

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to delete
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        PlatformDetails notOwningPlatform = samplePlatformDetails();
        notOwningPlatform.setId("dummy");

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(notOwningPlatform)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }

    @Test
    public void infoModelError() throws Exception {
        // Could not get Information models from Registry
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(null).when(mockRabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }

    @Test
    public void success() throws Exception {
        // Register platform successfully
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(samplePlatformResponseSuccess()).when(mockRabbitManager)
                .sendPlatformModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value(platformName));
    }

    @Test
    public void registryError() throws Exception {
        // Registry responds with error
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(samplePlatformResponseFail()).when(mockRabbitManager)
                .sendPlatformModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value(samplePlatformResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry responds with null
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doReturn(null).when(mockRabbitManager)
                .sendPlatformModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.OK)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendPlatformModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("Registry threw CommunicationException"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM responds with other ERROR
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(samplePlatformManagementResponse(ManagementStatus.ERROR)).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("AAM says that there was an ERROR"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doReturn(null).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(mockRabbitManager)
                .sendListInfoModelsRequest();
        doThrow(new CommunicationException("error")).when(mockRabbitManager)
                .sendManagePlatformRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("AAM threw CommunicationException: error"));
    }

    @Test
    public void invalidArguments() throws Exception {
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

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

        mockMvc.perform(post("/administration/user/cpanel/update_platform")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(platformDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.platformUpdateError")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.pl_update_error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.pl_update_error_description_description.length()")
                        .value(4))
                .andExpect(jsonPath("$.pl_update_error_description_description[1]")
                        .value("Length must be between 4 and 300 characters"))
                .andExpect(jsonPath("$.pl_update_error_description_description[3]")
                        .value("Length must be between 4 and 300 characters"));
    }
}