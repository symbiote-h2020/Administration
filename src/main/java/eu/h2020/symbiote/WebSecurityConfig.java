package eu.h2020.symbiote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import eu.h2020.symbiote.service.UserService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Configuration
	@Order(1)
	public static class UserWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

		@Bean
		public UserDetailsService mongoUserDetails() {
			return new UserService();
		}

		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {

			UserDetailsService userDetailsService = mongoUserDetails();
			auth.userDetailsService(userDetailsService);
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
					.logoutUrl("/user/logout")
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
