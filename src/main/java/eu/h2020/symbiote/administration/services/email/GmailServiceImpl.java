package eu.h2020.symbiote.administration.services.email;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import eu.h2020.symbiote.administration.model.GmailCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
public final class GmailServiceImpl implements GmailService {

    private static Log log = LogFactory.getLog(GmailServiceImpl.class);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final HttpTransport httpTransport;
    private String applicationName;
    private GmailCredentials gmailCredentials;

    @Autowired
    public GmailServiceImpl(GmailCredentials gmailCredentials,
                            @Value("${symbiote.core.administration.email.applicationName}") String applicationName)
            throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.gmailCredentials = gmailCredentials;

        Assert.notNull(applicationName,"Application Name can not be null!");
        this.applicationName = applicationName;
    }

    @Override
    public boolean sendMessage(String recipientAddress, String subject, String body) throws MessagingException,
            IOException {
        Message message = createMessageWithEmail(
                createEmail(recipientAddress, gmailCredentials.getUserEmail(), subject, body));

        return createGmail().users()
                .messages()
                .send(gmailCredentials.getUserEmail(), message)
                .execute()
                .getLabelIds().contains("SENT");
    }

    private Gmail createGmail() {
        Credential credential = authorize();
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        MimeMessage email = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);

        return new Message()
                .setRaw(Base64.encodeBase64URLSafeString(buffer.toByteArray()));
    }

    private Credential authorize() {
        return new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(gmailCredentials.getClientId(), gmailCredentials.getClientSecret())
                .build()
                .setAccessToken(gmailCredentials.getAccessToken())
                .setRefreshToken(gmailCredentials.getRefreshToken());
    }
}