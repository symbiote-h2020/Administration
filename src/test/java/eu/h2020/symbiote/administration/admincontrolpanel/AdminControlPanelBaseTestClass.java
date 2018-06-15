package eu.h2020.symbiote.administration.admincontrolpanel;

import eu.h2020.symbiote.administration.AdministrationBaseTestClass;
import eu.h2020.symbiote.administration.controllers.AdminCpanelController;
import eu.h2020.symbiote.administration.dummyListeners.DummyAAMListener;
import org.junit.Before;
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
public abstract class AdminControlPanelBaseTestClass extends AdministrationBaseTestClass {

    @Autowired
    protected ApplicationContext appContext;

    @Autowired
    protected WebApplicationContext wac;

    @Autowired
    protected Filter springSecurityFilterChain;

    @Autowired
    protected AdminCpanelController adminCpanelController;

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
    }
}