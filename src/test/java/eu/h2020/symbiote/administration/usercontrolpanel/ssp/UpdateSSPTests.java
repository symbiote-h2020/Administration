package eu.h2020.symbiote.administration.usercontrolpanel.ssp;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.Description;
import eu.h2020.symbiote.administration.model.SSPDetails;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class UpdateSSPTests extends UserControlPanelBaseTestClass {

    @Test
    public void infoModelError() throws Exception {
        // Could not get Information models from Registry
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(null).when(rabbitManager).sendListInfoModelsRequest();

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not retrieve the information models from registry"));
    }

    @Test
    public void doesNotOwnSSP() throws Exception {
        // The user does not own the ssp which tries to update
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        SSPDetails notOwningSSP = sampleSSPDetails("dummy");

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(notOwningSSP)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the smart space with id dummy"));
    }
    
    @Test
    public void success() throws Exception {
        // Register ssp successfully
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());
        doReturn(sampleSspRegistryResponseSuccess()).when(rabbitManager)
                .sendSmartSpaceModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value(ssp1Name));
    }

    @Test
    public void registryError() throws Exception {
        // Registry responds with error
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());
        doReturn(sampleSspRegistryResponseFail()).when(rabbitManager)
                .sendSmartSpaceModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value(sampleSspRegistryResponseFail().getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry responds with null
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());
        doReturn(null).when(rabbitManager)
                .sendSmartSpaceModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value("Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendSmartSpaceModificationRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value("Registry threw CommunicationException"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM responds with other ERROR
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.ERROR)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value("AAM says that there was an ERROR"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doReturn(null).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspUpdateError")
                        .value("AAM threw CommunicationException: error"));
    }

    @Test
    public void invalidArguments() throws Exception {
        // Invalid Arguments Check
        doReturn(sampleInformationModelListResponseSuccess()).when(rabbitManager)
                .sendListInfoModelsRequest();

        SSPDetails sspDetails = new SSPDetails(
                "a",
                "a",
                new ArrayList<>(Collections.singleton(new Description("a"))),
                "dummy",
                "dummy",
                null,
                null);


        mockMvc.perform(post("/administration/user/cpanel/update_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sspDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_id")
                        .value(serviceIdValidationMessage))
                .andExpect(jsonPath("$.error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.error_externalAddress")
                        .value(httpsUrlValidationMessage))
                .andExpect(jsonPath("$.error_siteLocalAddress")
                        .value(httpsUrlValidationMessage))
                .andExpect(jsonPath("$.error_exposingSiteLocalAddress")
                        .value(notNullValidationMessage));
    }
}