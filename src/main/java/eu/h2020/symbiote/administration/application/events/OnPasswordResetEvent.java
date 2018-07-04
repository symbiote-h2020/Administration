package eu.h2020.symbiote.administration.application.events;

import org.springframework.context.ApplicationEvent;

import java.util.Locale;

public class OnPasswordResetEvent extends ApplicationEvent {
    private final Locale locale;
    private final String username;
    private final String email;
    private final String password;

    public OnPasswordResetEvent(String username, Locale locale, String email, String password) {
        super(username);

        this.username = username;
        this.locale = locale;
        this.email = email;
        this.password = password;
    }

    public Locale getLocale() { return locale; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
