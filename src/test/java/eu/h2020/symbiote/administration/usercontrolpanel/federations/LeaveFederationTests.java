package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.Federation;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class LeaveFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void doesNotOwnPlatform() throws Exception {
        // The user does not own the platform which tries to leave the federation
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }


    @Test
    public void federationDoesNotExist() throws Exception {
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The federation does not exist"));
    }

    @Test
    public void platformIsOnlyMemberFederation() throws Exception {
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        Federation federation = sampleFederationRequest();
        federation.getMembers().remove(2);
        federation.getMembers().remove(1);
        federationRepository.save(federation);


        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Platform " + platformId +
                        " is the only a member of federation " + federationId + ". Please, delete the federation"));
    }

    @Test
    public void success() throws Exception {
        doReturn(sampleOwnedPlatformDetails()).when(mockRabbitManager)
                .sendOwnedPlatformDetailsRequest(any());

        Federation federation = sampleFederationRequest();
        federationRepository.save(federation);

        mockMvc.perform(post("/administration/user/cpanel/leave_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationId", federationId)
                .param("platformId", platformId + '3'))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationId + ".members.length()").value(2))
                .andExpect(jsonPath("$." + federationId + ".id").value(federationId));
    }
}