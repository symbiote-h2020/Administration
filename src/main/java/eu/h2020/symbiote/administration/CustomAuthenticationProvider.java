package eu.h2020.symbiote.administration;


import java.util.ArrayList;
import java.util.List;

import eu.h2020.symbiote.administration.exceptions.authentication.AAMProblemException;
import eu.h2020.symbiote.administration.exceptions.authentication.WrongAdminPasswordException;
import eu.h2020.symbiote.administration.exceptions.authentication.WrongUserNameException;
import eu.h2020.symbiote.administration.exceptions.authentication.WrongUserPasswordException;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.security.communication.payloads.UserDetailsResponse;
import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.administration.model.CoreUser;



/**
 * Spring authentication provider used during login.
 *
 * To authenticate a user, CustomAuthenticationProvider checks with Core AAM over RabbitMQ
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 * @author Vasileios Glykantzis (ICOM)
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    private static Log log = LogFactory.getLog(CustomAuthenticationProvider.class);


    private RabbitManager rabbitManager;
    private String adminUsername;
    private String adminPassword;

    @Autowired
    public CustomAuthenticationProvider(RabbitManager rabbitManager,
                                        @Value("${aam.deployment.owner.username}") String adminUsername,
                                        @Value("${aam.deployment.owner.password}") String adminPassword) {
        this.rabbitManager = rabbitManager;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public Authentication authenticate(Authentication authentication) 
      throws AuthenticationException {
  

        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Checking for admin
        if (name.equals(adminUsername) && password.equals(adminPassword)) {
            log.info("Valid Admin Username and password!");

            List<GrantedAuthority> grantedAuths = new ArrayList<>();

            // Todo: Are all the roles somewhere?
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

            CoreUser user = new CoreUser(name, password, UserRole.PLATFORM_OWNER,
                    true, true, true, true,
                    grantedAuths);

            // We clear the credential so that they are not shown anywhere
            user.clearPassword();
            return new UsernamePasswordAuthenticationToken(user, password, grantedAuths);
        }

        // Checking for normal user
        try{

            UserDetailsResponse response = rabbitManager.sendLoginRequest(new Credentials(name, password));

            if(response != null) {
                if (response.getHttpStatus() == HttpStatus.OK) {
                    log.info("Valid Username and password!");

                    List<GrantedAuthority> grantedAuths = new ArrayList<>();

                    // Todo: Are all the roles somewhere?
                    grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));

                    CoreUser user = new CoreUser(name, password, response.getUserDetails().getRole(),
                            true, true, true, true,
                            grantedAuths);

                    // We clear the credential so that they are not shown anywhere
                    user.clearPassword();
                    return new UsernamePasswordAuthenticationToken(user, password, grantedAuths);
                } else if (response.getHttpStatus() == HttpStatus.BAD_REQUEST) {
                    log.info("Username does not exist");
                    throw new WrongUserNameException();
                } else if (response.getHttpStatus() == HttpStatus.UNAUTHORIZED) {
                    log.info("Wrong user password");
                    throw new WrongUserPasswordException();
                } else if (response.getHttpStatus() == HttpStatus.FORBIDDEN){
                    log.info("Wrong admin password");
                    throw new WrongAdminPasswordException();
                }
            } else
                throw new AAMProblemException();
        } catch(CommunicationException e){
            log.debug(e.getMessage());
        }
        return null;
    }
 
    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    public void setRabbitManager(RabbitManager rabbitManager){
        this.rabbitManager = rabbitManager;
    }
}