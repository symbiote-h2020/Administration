package eu.h2020.symbiote.administration.usercontrolpanel;

import eu.h2020.symbiote.administration.AdministrationBaseTestClass;
import eu.h2020.symbiote.administration.CustomAuthenticationProvider;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.controllers.Register;
import eu.h2020.symbiote.administration.controllers.UserCpanel;
import eu.h2020.symbiote.administration.services.FederationService;
import eu.h2020.symbiote.administration.services.PlatformService;
import org.junit.Before;
import org.mockito.Mock;
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
    protected ApplicationContext appContext;

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected Filter springSecurityFilterChain;

    protected MockMvc mockMvc;

    @Mock
    protected RabbitManager mockRabbitManager;

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

        UserCpanel userCpanelController = appContext.getBean(UserCpanel.class);
        userCpanelController.setRabbitManager(mockRabbitManager);

        PlatformService platformService = appContext.getBean(PlatformService.class);
        platformService.setRabbitManager(mockRabbitManager);

        federationRepository.deleteAll();
    }
}