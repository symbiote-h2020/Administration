package eu.h2020.symbiote;


import java.util.ArrayList;
import java.util.List;

import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.security.communication.payloads.Credentials;
import eu.h2020.symbiote.model.CoreUser;
import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.jwt.JWTEngine;
import eu.h2020.symbiote.security.commons.jwt.JWTClaims;
import eu.h2020.symbiote.security.enums.CoreAttributes;
import eu.h2020.symbiote.security.commons.exceptions.custom.MalformedJWTException;



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
 
    @Autowired
    private RabbitManager rabbitManager;


    @Override
    public Authentication authenticate(Authentication authentication) 
      throws AuthenticationException {
  

        String name = authentication.getName();
        String password = authentication.getCredentials().toString();


        try{

            Token token = rabbitManager.sendLoginRequest(new Credentials(name, password));

            if(token != null){

                // When platform does not exist, we get null and the status is INACTIVE
                // Todo: integration with AAM is needed
                JWTClaims claims = JWTEngine.getClaimsFromToken(token.getToken());
                String platformId = claims.getAtt().get(CoreAttributes.OWNED_PLATFORM.toString());                

                List<GrantedAuthority> grantedAuths = new ArrayList<>();

                // Todo: Are all the roles somewhere?
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));

                // Todo: Change the user role
                CoreUser user = new CoreUser(name, password, UserRole.USER,true, true, true, true, grantedAuths, token);

                // We clear the credential so that they are not shown anywhere
                user.clearPassword();
                Authentication auth = new UsernamePasswordAuthenticationToken(user, null, grantedAuths);

                return auth;
            } 
        } catch(CommunicationException e){
            log.debug(e.getMessage());
        } catch(MalformedJWTException e){
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