package eu.h2020.symbiote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception { 
        auth
            .inMemoryAuthentication()
                .withUser("app").password("pass").roles("APP").and()
                .withUser("platform").password("pass").roles("PLATFORM").and()
                .withUser("admin").password("pass").roles("ADMIN");
    }

    @Configuration
    @Order(1)
    public static class AppWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/app/**")
                .authorizeRequests()
                    .antMatchers("/", "/home").permitAll()
                    .anyRequest().hasRole("APP")
                    .and()
                .formLogin()
                    .loginPage("/app/login")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/app/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedPage("/denied");
        }
    }

    @Configuration
    @Order(2)
    public static class PlatformWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        protected void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/platform/**")
                .authorizeRequests()
                    .antMatchers("/", "/home").permitAll()
                    .anyRequest().hasRole("PLATFORM")
                    .and()
                .formLogin()
                    .loginPage("/platform/login")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/platform/logout")
                    .logoutSuccessUrl("/")
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
                    .antMatchers("/", "/home").permitAll()
                    .anyRequest().hasRole("ADMIN")
                    .and()
                .formLogin()
                    .loginPage("/admin/login")
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

        // http
        //     .authorizeRequests()
        //         .antMatchers("/", "/home").permitAll()
        //         .antMatchers("/user/**").access("hasRole('USER')")
        //         .antMatchers("/platform/**").access("hasRole('PLATFORM')")
        //         .antMatchers("/admin/**").access("hasRole('ADMIN')")
        //         .anyRequest().authenticated()
        //         .and()
        //     .formLogin()
        //         .loginPage("/login")
        //         .permitAll()
        //         .and()
        //     .logout()
        //         .logoutSuccessUrl("/")
        //         .permitAll()
        //         .and()
        //     .exceptionHandling()
        //         .accessDeniedPage("/denied");