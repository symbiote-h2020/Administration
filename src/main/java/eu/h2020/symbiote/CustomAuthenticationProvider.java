package eu.h2020.symbiote;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.async.DeferredResult;

import eu.h2020.symbiote.security.payloads.Credentials;
import eu.h2020.symbiote.communication.RabbitManager;

import java.util.ArrayList;
import java.util.List;


@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
 
    @Autowired
    private RabbitManager rabbitManager;


    @Override
    public Authentication authenticate(Authentication authentication) 
      throws AuthenticationException {
            System.out.println("=================== DEBUG 1 ===================");
  
        final DeferredResult<String> deferredResult = new DeferredResult<>();

        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        rabbitManager.sendLoginRequest(new Credentials( name, password ), token ->{
                   
            System.out.println("Received response in interface: " + token);
            System.out.println("=================== DEBUG 2 ===================");

            deferredResult.setResult("test");
        });

        deferredResult.on
         
        if (deferredResult == "test") {

            System.out.println(deferredResult);
  
            // use the credentials
            // and authenticate against the third-party system
            
            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
            Authentication auth = new UsernamePasswordAuthenticationToken(name, password, grantedAuths);
            
            return auth;
        } else {
            return null;
        }
    }
 
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(
          UsernamePasswordAuthenticationToken.class);
    }
}