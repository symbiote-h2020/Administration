package eu.h2020.symbiote.administration.usercontrolpanel;

import eu.h2020.symbiote.administration.AdministrationBaseTestClass;
import eu.h2020.symbiote.administration.controllers.UserCpanelController;
import eu.h2020.symbiote.administration.services.*;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;


/**
 * Test class for use in testing MVC and form validation.
 */
public abstract class UserControlPanelBaseTestClass extends AdministrationBaseTestClass {

    @Autowired
    @InjectMocks
    protected ApplicationContext appContext;

    @Autowired
    @InjectMocks
    protected WebApplicationContext wac;

    @Autowired
    @InjectMocks
    protected Filter springSecurityFilterChain;

    @Autowired
    @InjectMocks
    protected UserCpanelController userCpanelController;

    @Autowired
    @InjectMocks
    protected OwnedServicesService ownedServicesService;

    @Autowired
    @InjectMocks
    protected PlatformService platformService;

    @Autowired
    @InjectMocks
    protected SSPService sspService;

    @Autowired
    @InjectMocks
    protected InformationModelService informationModelService;

    @Autowired
    @InjectMocks
    protected FederationNotificationService federationNotificationService;

    @Autowired
    @InjectMocks
    protected CheckServiceOwnershipService checkServiceOwnershipService;

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
    }
}