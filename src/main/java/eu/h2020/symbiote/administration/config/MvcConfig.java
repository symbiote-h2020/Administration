package eu.h2020.symbiote.administration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter {

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        // These are added because there are no controllers for these urls
        registry.addViewController("/administration/home").setViewName("index");
        registry.addViewController("/administration").setViewName("index");
        registry.addViewController("/administration/").setViewName("index");
    }

    @Bean
    public TemplateResolver templateResolver() {
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML5");
        return templateResolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/administration/webjars/**").addResourceLocations(
                "classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/administration/**").addResourceLocations(
                CLASSPATH_RESOURCE_LOCATIONS);

    }
}