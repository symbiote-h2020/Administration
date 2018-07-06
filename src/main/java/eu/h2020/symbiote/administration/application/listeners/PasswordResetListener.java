package eu.h2020.symbiote.administration.application.listeners;

import eu.h2020.symbiote.administration.application.events.OnPasswordResetEvent;
import eu.h2020.symbiote.administration.services.email.GmailService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class PasswordResetListener implements ApplicationListener<OnPasswordResetEvent> {

    private static Log log = LogFactory.getLog(PasswordResetListener.class);

    private MessageSource messages;
    private GmailService gmailService;
    private String administrationUrl;

    @Autowired
    public PasswordResetListener(MessageSource messages,
                                 GmailService gmailService,
                                 @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress) {
        this.messages = messages;
        this.gmailService = gmailService;

        Assert.notNull(coreInterfaceAddress,"coreInterfaceAddress can not be null!");
        this.administrationUrl = coreInterfaceAddress.replace("coreInterface", "administration");
    }

    @Override
    public void onApplicationEvent(OnPasswordResetEvent event) {
        this.sendPasswordResetEmail(event);
    }

    private void sendPasswordResetEmail(OnPasswordResetEvent event) {
        try {
            log.debug("Received password reset event = " + event);

            String recipientAddress = event.getEmail();
            String subject = "Password Reset";
            String message = messages.getMessage("message.passwordReset", null, event.getLocale()) +
                    " " + event.getPassword() + ". " + messages.getMessage("message.changePassword", null, event.getLocale());

            gmailService.sendMessage(recipientAddress, subject, message);
        } catch (Throwable e) {
            log.warn("Exception thrown during passwordResetEvent", e);
        }
    }
}