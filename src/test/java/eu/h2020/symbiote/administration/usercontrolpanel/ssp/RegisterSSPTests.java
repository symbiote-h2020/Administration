package eu.h2020.symbiote.administration.usercontrolpanel.ssp;

import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.SSPDetails;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.ManagementStatus;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class RegisterSSPTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        // Register ssp successfully
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.OK)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value(sspName));
    }
    
    @Test
    public void sspExists() throws Exception {
        // AAM responds with PLATFORM_EXISTS
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.PLATFORM_EXISTS)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sspRegistrationError")
                        .value("AAM says that the SSP exists!"));
    }

    @Test
    public void aamError() throws Exception {
        // AAM responds with other ERROR
        doReturn(sampleSmartSpaceManagementResponse(ManagementStatus.ERROR)).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sspRegistrationError")
                        .value("AAM says that there was an ERROR"));
    }

    @Test
    public void aamTimeout() throws Exception {
        // AAM responds with null
        doReturn(null).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspRegistrationError")
                        .value("AAM unreachable!"));
    }

    @Test
    public void aamCommunicationException() throws Exception {
        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(rabbitManager)
                .sendManageSSPRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleSSPDetails())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.sspRegistrationError")
                        .value("AAM threw CommunicationException: error"));
    }

    @Test
    public void invalidArguments() throws Exception {
        // Invalid Arguments Check

        SSPDetails sspDetails = new SSPDetails("a", "a", "dummy", "dummy", null);


        mockMvc.perform(post("/administration/user/cpanel/register_ssp")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sspDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_id")
                        .value("must match \"^(\\Z|[\\w-]{4,})$\""))
                .andExpect(jsonPath("$.error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.error_externalAddress")
                        .value("must match \"^(https:\\/\\/www\\.|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}" +
                                "[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$\""))
                .andExpect(jsonPath("$.error_siteLocalAddress")
                        .value("must match \"^(https:\\/\\/www\\.|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}" +
                                "[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$\""))
                .andExpect(jsonPath("$.error_exposingSiteLocalAddress")
                        .value("may not be null"));
    }
}