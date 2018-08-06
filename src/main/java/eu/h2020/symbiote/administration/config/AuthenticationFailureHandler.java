package eu.h2020.symbiote.administration.config;

import eu.h2020.symbiote.administration.exceptions.authentication.CustomAuthenticationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static Log log = LogFactory.getLog(AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {

        log.debug("Authentication failure", e);
        response.setStatus(((CustomAuthenticationException)e).getHttpStatus());

        PrintWriter writer = response.getWriter();
        writer.write(e.getMessage());
        writer.flush();
    }
}
