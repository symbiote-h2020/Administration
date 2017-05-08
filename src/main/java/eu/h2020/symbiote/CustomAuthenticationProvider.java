package eu.h2020.symbiote;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import eu.h2020.symbiote.communication.RabbitManager;
import eu.h2020.symbiote.communication.CommunicationException;
import eu.h2020.symbiote.security.payloads.Credentials;
import eu.h2020.symbiote.security.token.Token;

import java.util.ArrayList;
import java.util.List;


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

            Token token = rabbitManager.sendLoginRequest(new Credentials( name, password ));
            
            if(token != null){

                List<GrantedAuthority> grantedAuths = new ArrayList<>();
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
                Authentication auth = new UsernamePasswordAuthenticationToken(name, password, grantedAuths);
                
                return auth;

            } 
        } catch(CommunicationException e){
            log.debug(e.getMessage());
        }
        return null;
    }
 
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(
          UsernamePasswordAuthenticationToken.class);
    }
}