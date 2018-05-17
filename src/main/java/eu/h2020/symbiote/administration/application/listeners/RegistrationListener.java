package eu.h2020.symbiote.administration.application.listeners;

import eu.h2020.symbiote.administration.application.events.OnRegistrationCompleteEvent;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.services.user.IUserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.UUID;

@Component
public class RegistrationListener implements
        ApplicationListener<OnRegistrationCompleteEvent> {

    private static Log log = LogFactory.getLog(RegistrationListener.class);

    private IUserService userService;
    private MessageSource messages;
    private JavaMailSender mailSender;
    private String administrationUrl;

    @Autowired
    public RegistrationListener(IUserService userService, MessageSource messages, JavaMailSender mailSender,
                                @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress) {
        this.userService = userService;
        this.messages = messages;
        this.mailSender = mailSender;

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

            String recipientAddress = user.getRecoveryMail();
            String subject = "Registration Confirmation";
            String confirmationUrl
                    = event.getAppUrl() + "/registrationConfirm.html?token=" + token;
            String message = messages.getMessage("message.regSucc", null, event.getLocale());

            log.debug("confirmUrl = " + confirmationUrl);

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(recipientAddress);
            email.setSubject(subject);
            email.setText(message + administrationUrl + confirmationUrl);
            mailSender.send(email);
        } catch (Throwable e) {
            log.warn("Exception thrown during registrationEvent", e);
        }
    }
}