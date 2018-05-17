package eu.h2020.symbiote.administration.usercontrolpanel;

import eu.h2020.symbiote.administration.AdministrationBaseTestClass;
import eu.h2020.symbiote.administration.controllers.UserCpanelController;
import eu.h2020.symbiote.administration.dummyListeners.DummyAAMListener;
import eu.h2020.symbiote.administration.services.federation.FederationNotificationService;
import eu.h2020.symbiote.administration.services.infomodel.InformationModelService;
import eu.h2020.symbiote.administration.services.ownedservices.CheckServiceOwnershipService;
import eu.h2020.symbiote.administration.services.ownedservices.OwnedServicesService;
import eu.h2020.symbiote.administration.services.platform.PlatformService;
import eu.h2020.symbiote.administration.services.ssp.SSPService;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;


/**
 * Test class for use in testing MVC and form validation.
 */
public abstract class UserControlPanelBaseTestClass extends AdministrationBaseTestClass {

    @Autowired
    protected Environment environment;

    @Autowired
    protected ApplicationContext appContext;

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected Filter springSecurityFilterChain;

    @Autowired
    protected UserCpanelController userCpanelController;

    @Autowired
    protected OwnedServicesService ownedServicesService;

    @Autowired
    protected PlatformService platformService;

    @Autowired
    protected SSPService sspService;

    @Autowired
    protected InformationModelService informationModelService;

    @Autowired
    protected FederationNotificationService federationNotificationService;

    @Autowired
    protected CheckServiceOwnershipService checkServiceOwnershipService;

    @Autowired
    protected DummyAAMListener dummyAAMListener;

    protected MockMvc mockMvc;


    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(this.wac)
            .addFilters(springSecurityFilterChain)
            .build();

        MockitoAnnotations.initMocks(this);

        originalRequestFactory = restTemplate.getRequestFactory();

        federationRepository.deleteAll();

        dummyAAMListener.clearMessagesReceivedByListener();

        for (final String profileName : environment.getActiveProfiles()) {
            System.out.println("Currently active profile - " + profileName);
        }
    }
}