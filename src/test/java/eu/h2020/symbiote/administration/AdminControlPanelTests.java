package eu.h2020.symbiote.administration;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.controllers.AdminCpanel;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.administration.model.CreateFederationRequest;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import eu.h2020.symbiote.security.communication.payloads.FederationRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class AdminControlPanelTests extends AdministrationTests {

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

        AdminCpanel adminCpanelController = appContext.getBean(AdminCpanel.class);
        adminCpanelController.setRabbitManager(mockRabbitManager);
    }



    @Test
    public void getControlPanelDenied() throws Exception {

        mockMvc.perform(get("/admin/cpanel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/admin/login"));
    }

    @Test
    public void getControlPanelSuccess() throws Exception {

        mockMvc.perform(get("/admin/cpanel")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER))) )
            .andExpect(status().isOk());

    }


    @Test
    public void createFederation() throws Exception {

        CreateFederationRequest request = new CreateFederationRequest();
        request.setId(federationRuleId);
        request.setPlatform1Id(platformId);
        request.setPlatform2Id(platformId + '2');

        // Create Federation successfully
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendFederationRuleManagementRequest(any());


        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"));

        // Not both platforms ids are present in AAM response
        request.setPlatform2Id(platformId + "3");

        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not both platforms ids present in AAM response"));

        // AAM response contains more than 1 federation rule
        Map<String, FederationRule> responseWith2Rules = new HashMap<>();
        responseWith2Rules.put("1", sampleFederationRuleManagementResponse().get(federationRuleId));
        responseWith2Rules.put("2", sampleFederationRuleManagementResponse().get(federationRuleId));

        doReturn(responseWith2Rules).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Contains more than 1 Federation rule"));

        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable"));

        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM threw communication exception: error"));

        // Invalid CreateFederationRequest
        request.setId("a");
        request.setPlatform1Id("a");
        request.setPlatform2Id("b");

        mockMvc.perform(post("/admin/cpanel/create_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"))
                .andExpect(jsonPath("$.federation_reg_error_id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""))
                .andExpect(jsonPath("$.federation_reg_error_platform1Id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""))
                .andExpect(jsonPath("$.federation_reg_error_platform2Id")
                        .value("must match \"^([\\w-][\\w-][\\w-][\\w-]+)\""));
    }


    @Test
    public void listFederations() throws Exception {

        // Successfully listing Federations
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/list_federations")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationRuleId + ".platformIds.length()").value(2))
                .andExpect(jsonPath("$." + federationRuleId + ".federationId").value(federationRuleId));

        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/list_federations")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable during ListFederationRequest"));

        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/list_federations")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("AAM threw communication exception during ListFederationRequest: error"));
    }


    @Test
    public void deleteFederation() throws Exception {

        // Successfully deleted Federation
        doReturn(sampleFederationRuleManagementResponse()).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationRuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + federationRuleId + ".platformIds.length()").value(2))
                .andExpect(jsonPath("$." + federationRuleId + ".federationId").value(federationRuleId));

        // AAM responds with null
        doReturn(null).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationRuleId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AAM unreachable during DeleteFederationRequest"));

        // AAM throws CommunicationException
        doThrow(new CommunicationException("error")).when(mockRabbitManager).sendFederationRuleManagementRequest(any());

        mockMvc.perform(post("/admin/cpanel/delete_federation")
                .with(authentication(sampleAdminAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .param("federationIdToDelete", federationRuleId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("AAM threw communication exception during DeleteFederationRequest: error"));
    }

}