package eu.h2020.symbiote.administration.services.email;

import javax.mail.MessagingException;
import java.io.IOException;

public interface GmailService {
    boolean sendMessage(String recipientAddress, String subject, String body) throws MessagingException, IOException;
}
