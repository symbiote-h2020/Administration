package eu.h2020.symbiote.administration.usercontrolpanel.federations;

import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.model.mim.*;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class CreateFederationTests extends UserControlPanelBaseTestClass {

    @Test
    public void success() throws Exception {
        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(sampleFederationRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Federation Registration was successful!"))
                .andExpect(jsonPath("$.federation.id").value(federationId));

        List<Federation> federations = federationRepository.findAll();
        assertEquals(1, federations.size());
        assertEquals(federationId, federations.get(0).getId());
    }

    @Test
    public void federationExists() throws Exception {

        // Save the federation
        Federation federation = sampleFederationRequest();
        federationRepository.save(federation);

        // Change the federation name and send creation request
        federation.setName("newName");

        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(federation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The federation with id '" + federationId +
                        "' already exists!"));
    }

    @Test
    public void invalidCreateFederationRequest() throws Exception {

        // Invalid FederationRequest
        Federation federation = new Federation();
        federation.setId("invalid.id");
        federation.setName("na");


        InformationModel informationModel = new InformationModel();
        informationModel.setId("invalid.id");
        federation.setInformationModel(informationModel);

        QoSConstraint qosConstraint1 = new QoSConstraint();
        qosConstraint1.setMetric(QoSMetric.availability);
        qosConstraint1.setComparator(Comparator.equal);
        QoSConstraint qosConstraint2 = new QoSConstraint();
        qosConstraint2.setThreshold(1.2);

        List<QoSConstraint> qosConstraints = new ArrayList<>(Arrays.asList(qosConstraint1, qosConstraint2));
        federation.setSlaConstraints(qosConstraints);

        FederationMember member1 = new FederationMember("valid", platformUrl);
        FederationMember member2 = new FederationMember("invalid.", platformUrl + "2");
        List<FederationMember> members = new ArrayList<>(Arrays.asList(member1, member2));
        federation.setMembers(members);


        mockMvc.perform(post("/administration/user/cpanel/create_federation")
                .with(authentication(sampleUserAuth(UserRole.PLATFORM_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(federation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Arguments"))
                .andExpect(jsonPath("$.error_id")
                        .value("must match \"^(\\Z|[\\w-]{4,})$\""))
                .andExpect(jsonPath("$.error_name")
                        .value("Length must be between 3 and 30 characters"))
                .andExpect(jsonPath("$.error_informationModel_id")
                        .value("must match \"^[\\w-]{4,}$\""))
                .andExpect(jsonPath("$.error_slaConstraints_metric[1]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_slaConstraints_comparator[1]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_slaConstraints_threshold[0]")
                        .value("may not be null"))
                .andExpect(jsonPath("$.error_members_platformId[1]")
                        .value("must match \"^[\\w-]{4,}$\""));

        assertEquals(0, federationRepository.findAll().size());
    }
}