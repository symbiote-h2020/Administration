package eu.h2020.symbiote.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


/**
 * Spring configuration for security.
 *
 * Two different restricted areas are created, one for users and one for administrators.
 * During login, CustomAuthenticationProvider is used
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 * @author Vasileios Glykantzis (ICOM)
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

            auth.authenticationProvider(authProvider).eraseCredentials(false);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/**").cors();

            http
                .antMatcher("/administration/user/**")
                .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage("/administration/user/login")
                    .defaultSuccessUrl("/administration/user/cpanel")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/administration/user/logout")
                    .logoutSuccessUrl("/administration")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/administration/denied")
                    .and()
                .sessionManagement()
                    .invalidSessionUrl("/administration/user/login")
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        }
    }

    @Configuration                                                   
    public static class AdminWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.antMatcher("/**").cors();

            http
                .antMatcher("/administration/admin/**")
                .authorizeRequests()
                    .anyRequest().hasRole("ADMIN")
                    .and()
                .formLogin()
                    .loginPage("/administration/admin/login")
                    .defaultSuccessUrl("/administration/admin/cpanel")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/administration/admin/logout")
                    .logoutSuccessUrl("/administration")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/administration/denied")
                    .and()
                .sessionManagement()
                    .invalidSessionUrl("/administration/admin/login")
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

            http.antMatcher("/**").cors();
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "OPTIONS", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Origin", "X-Requested-With", "Content-Type", "Accept", "x-xsrf-token"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
