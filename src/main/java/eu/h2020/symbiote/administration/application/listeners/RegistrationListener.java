package eu.h2020.symbiote.administration.application.listeners;

import eu.h2020.symbiote.administration.application.events.OnRegistrationCompleteEvent;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.services.email.GmailService;
import eu.h2020.symbiote.administration.services.user.UserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.UUID;

@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    private static Log log = LogFactory.getLog(RegistrationListener.class);

    private UserService userService;
    private MessageSource messages;
    private GmailService gmailService;
    private String administrationUrl;

    @Autowired
    public RegistrationListener(UserService userService,
                                MessageSource messages,
                                GmailService gmailService,
                                @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress) {
        this.userService = userService;
        this.messages = messages;
        this.gmailService = gmailService;

        Assert.notNull(coreInterfaceAddress,"coreInterfaceAddress can not be null!");
        this.administrationUrl = coreInterfaceAddress.replace("coreInterface", "administration");
    }

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        this.confirmRegistration(event);
    }

    private void confirmRegistration(OnRegistrationCompleteEvent event) {
        try {
            log.debug("Received registration event = " + event);

            CoreUser user = event.getUser();
            String token = UUID.randomUUID().toString();

            log.debug("Created " + token + " for user = " + user);

            userService.createVerificationToken(user, token);

            String recipientAddress = event.getEmail();
            String subject = "Registration Confirmation";
            String confirmationUrl = event.getAppUrl() + "/registrationConfirm.html?token=" + token;
            String message = messages.getMessage("message.successfulRegistration", null, event.getLocale()) +
                    " " + administrationUrl + confirmationUrl;

            log.debug(" confirmUrl = " + confirmationUrl);
            gmailService.sendMessage(recipientAddress, subject, message);
        } catch (Throwable e) {
            log.warn("Exception thrown during registrationEvent", e);
        }
    }
}