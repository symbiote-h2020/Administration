package eu.h2020.symbiote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import eu.h2020.symbiote.CustomAuthenticationProvider;


/**
 * Spring configuration for security.
 *
 * Two different restricted areas are created, one for users and one for administrators.
 * During login, CustomAuthenticationProvider is used
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    // In order to support multiple security domains (WebSecurityConfigurerAdapter) we need the Order Annotation
    @Configuration
    @Order(1)
    public static class UserWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private CustomAuthenticationProvider authProvider;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {

            auth.authenticationProvider(authProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/user/**")
                .authorizeRequests()
                    .anyRequest().authenticated()
                    // .anyRequest().hasRole("USER")
                    .and()
                .formLogin()
                    .loginPage("/user/login")
                    .defaultSuccessUrl("/user/cpanel")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/")
                    .logoutSuccessUrl("/")
//                    .deleteCookies("JSESSIONID")
//                    .invalidateHttpSession(true)
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/denied");
        }
    }

    @Configuration                                                   
    public static class AdminWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/admin/**")
                .authorizeRequests()
                    .anyRequest().hasRole("admin")
                    .and()
                .formLogin()
                    .loginPage("/admin/login")
                    .defaultSuccessUrl("/admin/cpanel")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/denied");
        }
    }
}
