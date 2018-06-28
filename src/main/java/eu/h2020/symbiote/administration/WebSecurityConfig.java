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
import java.util.Collections;


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
        protected void configure(AuthenticationManagerBuilder auth) {

            auth.authenticationProvider(authProvider).eraseCredentials(false);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.cors().and()
                .antMatcher("/administration/user/**")
                .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage("/administration/user/login")
                    .defaultSuccessUrl("/administration")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/administration/user/logout")
                    .logoutSuccessUrl("/administration")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/administration")
                    .and()
                .sessionManagement()
                    .invalidSessionUrl("/administration")
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        }
    }

    @Configuration
    @Order(2)
    public static class AdminWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.cors().and()
                .antMatcher("/administration/admin/**")
                .authorizeRequests()
                    .anyRequest().hasRole("ADMIN")
                    .and()
                .formLogin()
                    .loginPage("/administration/admin/login")
                    .defaultSuccessUrl("/administration")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/administration/admin/logout")
                    .logoutSuccessUrl("/administration")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/administration")
                    .and()
                .sessionManagement()
                    .invalidSessionUrl("/administration")
                    .and()
                .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        }
    }

    @Configuration
    @Order(3)
    public static class ComponentControllerWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.antMatcher("/administration/generic/**").cors().and()
                    .csrf().disable();
        }
    }

    @Configuration
    @Order(4)
    public static class GeneralWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http.antMatcher("/**").cors().and()
                    .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
            }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "OPTIONS", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Origin", "X-Requested-With", "Content-Type", "Accept", "x-xsrf-token"));
        configuration.setExposedHeaders(Collections.singletonList("Content-Disposition"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
