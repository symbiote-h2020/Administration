package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PlatformConfigurer {

    private static Log log = LogFactory.getLog(PlatformConfigurer.class);

    private ResourceLoader resourceLoader;
    private String coreInterfaceAddress;
    private String paamValidityMillis;
    private String cloudCoreInterfaceAddress;

    @Autowired
    public PlatformConfigurer(ResourceLoader resourceLoader,
                              @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress,
                              @Value("${paam.deployment.token.validityMillis}") String paamValidityMillis) {

        this.resourceLoader = resourceLoader;

        Assert.notNull(coreInterfaceAddress,"coreInterfaceAddress can not be null!");
        this.coreInterfaceAddress = coreInterfaceAddress;

        Assert.notNull(paamValidityMillis,"paamValidityMillis can not be null!");
        this.paamValidityMillis = paamValidityMillis;

        this.cloudCoreInterfaceAddress = this.coreInterfaceAddress
                .replace("8100", "8101")
                .replace("coreInterface", "cloudCoreInterface");
    }


    public void returnPlatformConfiguration(HttpServletResponse response,
                                            CoreUser user,
                                            OwnedService platformDetails,
                                            PlatformConfigurationMessage configurationMessage) throws Exception {

        String platformId = configurationMessage.getPlatformId();
        String platformOwnerUsername = configurationMessage.getPlatformOwnerUsername();
        String platformOwnerPassword = configurationMessage.getPlatformOwnerPassword();
        String platformOwnerUsernameInCore = user.getUsername();
        String platformOwnerPasswordInCore = user.getPassword();
        String componentKeystorePassword = configurationMessage.getComponentsKeystorePassword().isEmpty() ?
                "pass" : configurationMessage.getComponentsKeystorePassword();
        String aamKeystoreName = configurationMessage.getAamKeystoreName().isEmpty() ?
                "paam-keystore-" + platformDetails.getInstanceFriendlyName() :
                configurationMessage.getAamKeystoreName();
        String aamKeystorePassword = configurationMessage.getAamKeystorePassword().isEmpty() ?
                "pass" : configurationMessage.getAamKeystorePassword();
        // ToDo: Fix that to getAaaKeyPassword when jdk fix is published
        String aamPrivateKeyPassword = configurationMessage.getAamKeystorePassword().isEmpty() ?
                "pass" : configurationMessage.getAamKeystorePassword();
        String tokenValidity = configurationMessage.getTokenValidity() == 0 ? paamValidityMillis :
                configurationMessage.getTokenValidity().toString();
        Boolean useBuiltInRapPlugin = configurationMessage.getUseBuiltInRapPlugin();
        PlatformConfigurationMessage.Level level = configurationMessage.getLevel();

        // Create .zip output stream
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"configuration.zip\"");
        response.addHeader("Content-Type", "application/zip");

        configureCloudConfigProperties(platformDetails, zipOutputStream, useBuiltInRapPlugin);
        configureNginx(zipOutputStream, platformDetails, level);
        configureComponentProperties(zipOutputStream, "RegistrationHandler", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);
        configureComponentProperties(zipOutputStream, "ResourceAccessProxy", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);
        configureComponentProperties(zipOutputStream, "Monitoring", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);

        if (level == PlatformConfigurationMessage.Level.L2) {
            configureComponentProperties(zipOutputStream, "FederationManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);
            configureComponentProperties(zipOutputStream, "SubscriptionManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);
            configureComponentProperties(zipOutputStream, "PlatformRegistry", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails);
        }

        configureAAMProperties(zipOutputStream, platformOwnerUsername, platformOwnerPassword, aamKeystoreName,
                aamKeystorePassword, aamPrivateKeyPassword, tokenValidity);
        configureCertProperties(zipOutputStream, platformId, platformOwnerUsernameInCore,
                platformOwnerPasswordInCore, aamKeystoreName, aamKeystorePassword, this.coreInterfaceAddress);

        zipOutputStream.close();
        response.getOutputStream().close();
    }

    private void configureCloudConfigProperties(OwnedService platformDetails, ZipOutputStream zipOutputStream,
                                                Boolean useBuiltInRapPlugin)
            throws Exception {

        // Loading application.properties
        InputStream propertiesResourceAsStream = resourceLoader
                .getResource("classpath:files/CloudConfigProperties/application.properties").getInputStream();
        String applicationProperties = new BufferedReader(new InputStreamReader(propertiesResourceAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the application.properties file accordingly
        // Platform Details Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^(platform.id=).*$",
                "platform.id=" + Matcher.quoteReplacement(platformDetails.getServiceInstanceId()));

        // AMQP Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^(rabbit.host=).*$",
                "rabbit.host=localhost");
        applicationProperties = applicationProperties.replaceFirst("(?m)^(rabbit.username=).*$",
                "rabbit.username=guest");
        applicationProperties = applicationProperties.replaceFirst("(?m)^(rabbit.password=).*$",
                "rabbit.password=guest");

        // Necessary Urls Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.core.interface.url=).*$",
                "symbIoTe.core.interface.url=" + Matcher.quoteReplacement(this.coreInterfaceAddress));
        applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.core.cloud.interface.url=).*$",
                "symbIoTe.core.cloud.interface.url=" + Matcher.quoteReplacement(this.cloudCoreInterfaceAddress));
        applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.interworking.interface.url=).*$",
                "symbIoTe.interworking.interface.url=" +
                        Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()) +
                        "/cloudCoreInterface");
        applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.localaam.url=).*$",
                "symbIoTe.localaam.url=" + Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()) +
                        "/paam");

        // RAP Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^.*(rap.enableSpecificPlugin=).*$",
                "ResourceAccessProxy.enableSpecificPlugin=" + useBuiltInRapPlugin.booleanValue());

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("CloudConfigProperties/application.properties"));
        InputStream stream = new ByteArrayInputStream(applicationProperties.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureNginx(ZipOutputStream zipOutputStream, OwnedService platformDetails,
                                PlatformConfigurationMessage.Level level)
            throws Exception {

        StringBuilder sb = new StringBuilder("classpath:files/nginx_l");

        if (level == PlatformConfigurationMessage.Level.L1)
            sb.append("1.conf");
        else if (level == PlatformConfigurationMessage.Level.L2)
            sb.append("2.conf");

        String nginxFile = sb.toString();

        // Loading nginx.conf
        InputStream nginxConfAsStream = resourceLoader
                .getResource(nginxFile).getInputStream();
        String nginxConf = new BufferedReader(new InputStreamReader(nginxConfAsStream))
                .lines().collect(Collectors.joining("\n"));

        Pattern p = Pattern.compile(":(\\d)+(\\/)?");   // the pattern to search for
        Matcher m = p.matcher(platformDetails.getPlatformInterworkingInterfaceAddress());

        // if we find a match, get the group
        if (m.find()) {
            // we are only looking for the first occurrence
            String platformPort = m.group(0).replaceAll(":", "").replaceAll("/", "");
            log.debug("The platform used the port: " + platformPort);
            nginxConf = nginxConf.replaceFirst("(?m)^.*(listen 443 ssl;).*$",
                    "        listen " + platformPort + " ssl;  ## HTTPS");
        }

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https:\\/\\/\\{symbiote-core-hostname\\}\\/cloudCoreInterface).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.cloudCoreInterfaceAddress) + "/;");
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https:\\/\\/\\{symbiote-core-hostname\\}\\/coreInterface).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.coreInterfaceAddress) + "/;");

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("nginx.conf"));
        InputStream stream = new ByteArrayInputStream(nginxConf.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureComponentProperties(ZipOutputStream zipOutputStream, String componentFolder,
                                              String platformOwnerUsername, String platformOwnerPassword,
                                              String keystorePassword, String platformId,
                                              OwnedService platformDetails) throws Exception {

        // Loading component properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/" + componentFolder + "/bootstrap.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.username=).*$",
                "symbIoTe.component.username=" + Matcher.quoteReplacement(platformOwnerUsername));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.password=).*$",
                "symbIoTe.component.password=" + Matcher.quoteReplacement(platformOwnerPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.keystore.password=).*$",
                "symbIoTe.component.keystore.password=" + Matcher.quoteReplacement(keystorePassword));

        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.core.interface.url=).*$",
                "symbIoTe.core.interface.url=" + Matcher.quoteReplacement(this.coreInterfaceAddress));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.localaam.url=).*$",
                "symbIoTe.localaam.url=" + Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()) +
                        "/paam");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(platform.id=).*$",
                "platform.id=" + Matcher.quoteReplacement(platformId));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbiote.notification.url=).*$",
                "symbiote.notification.url=" + Matcher.quoteReplacement(this.cloudCoreInterfaceAddress) + "/accessNotifications");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.interworking.interface.url=).*$",
                "symbIoTe.interworking.interface.url=" + Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()));

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry(componentFolder + "/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureAAMProperties(ZipOutputStream zipOutputStream, String serviceOwnerUsername,
                                        String serviceOwnerPassword, String aamKeystoreName,
                                        String aamKeystorePassword, String aamPrivateKeyPassword,
                                        String tokenValidity)
            throws Exception {

        // Loading AAM bootstrap.properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/AuthenticationAuthorizationManager/bootstrap.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.owner.username=).*$",
                "aam.deployment.owner.username=" + Matcher.quoteReplacement(serviceOwnerUsername));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.owner.password=).*$",
                "aam.deployment.owner.password=" + Matcher.quoteReplacement(serviceOwnerPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.KEY_STORE_FILE_NAME=).*$",
                "aam.security.KEY_STORE_FILE_NAME=file:///#{systemProperties['user.dir']}/" +
                        Matcher.quoteReplacement(aamKeystoreName) + ".p12");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.KEY_STORE_PASSWORD=).*$",
                "aam.security.KEY_STORE_PASSWORD=" + Matcher.quoteReplacement(aamKeystorePassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.PV_KEY_PASSWORD=).*$",
                "aam.security.PV_KEY_PASSWORD=" + Matcher.quoteReplacement(aamPrivateKeyPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.token.validityMillis=).*$",
                "aam.deployment.token.validityMillis=" + Matcher.quoteReplacement(tokenValidity));

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("AuthenticationAuthorizationManager/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureCertProperties(ZipOutputStream zipOutputStream, String serviceId,
                                         String serviceOwnerUsernameInCore,
                                         String serviceOwnerPasswordInCore,
                                         String aamKeystoreName, String aamKeystorePassword,
                                         String coreAAMAddress)
            throws Exception {

        // Loading PlatformAAMCertificateKeyStoreFactory
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/AuthenticationAuthorizationManager/cert.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(coreAAMAddress=).*$",
                "coreAAMAddress=" + Matcher.quoteReplacement(coreAAMAddress));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(serviceOwnerUsername=).*$",
                "serviceOwnerUsername=" + Matcher.quoteReplacement(serviceOwnerUsernameInCore));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(serviceOwnerPassword=).*$",
                "serviceOwnerPassword=" + Matcher.quoteReplacement(serviceOwnerPasswordInCore));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(serviceId=).*$",
                "serviceId=" + Matcher.quoteReplacement(serviceId));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(keyStoreFileName=).*$",
                "keyStoreFileName=" + Matcher.quoteReplacement(aamKeystoreName) +".p12");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(keyStorePassword=).*$",
                "keyStorePassword=" + Matcher.quoteReplacement(aamKeystorePassword));


        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("AuthenticationAuthorizationManager/cert.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }
}
