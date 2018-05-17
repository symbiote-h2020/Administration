package eu.h2020.symbiote.administration.application.events;

import eu.h2020.symbiote.administration.model.CoreUser;
import org.springframework.context.ApplicationEvent;

import java.util.Locale;

public class OnRegistrationCompleteEvent extends ApplicationEvent {
    private String appUrl;
    private Locale locale;
    private CoreUser user;

    public OnRegistrationCompleteEvent(
            CoreUser user, Locale locale, String appUrl) {
        super(user);

        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
    }

    public String getAppUrl() { return appUrl; }
    public Locale getLocale() { return locale; }
    public CoreUser getUser() { return user; }
}
