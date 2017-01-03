package eu.h2020.symbiote;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/home").setViewName("home");
        registry.addViewController("/").setViewName("home");
        // registry.addViewController("/app/cpanel").setViewName("cpanel");
        registry.addViewController("/app/login").setViewName("login");
        // registry.addViewController("/platform/cpanel").setViewName("cpanel");
        registry.addViewController("/platform/login").setViewName("login");
        // registry.addViewController("/admin/cpanel").setViewName("cpanel");
        registry.addViewController("/admin/login").setViewName("login");
        registry.addViewController("/denied").setViewName("denied");
    }

}