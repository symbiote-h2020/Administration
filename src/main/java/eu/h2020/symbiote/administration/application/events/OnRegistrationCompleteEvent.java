package eu.h2020.symbiote.administration.application.events;

import eu.h2020.symbiote.administration.model.CoreUser;
import org.springframework.context.ApplicationEvent;

import java.util.Locale;

public class OnRegistrationCompleteEvent extends ApplicationEvent {
    private final String appUrl;
    private final Locale locale;
    private final CoreUser user;
    private final String email;

    public OnRegistrationCompleteEvent(
            CoreUser user, Locale locale, String appUrl, String email) {
        super(user);

        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
        this.email = email;
    }

    public String getAppUrl() { return appUrl; }
    public Locale getLocale() { return locale; }
    public CoreUser getUser() { return user; }
    public String getEmail() { return email; }
}
