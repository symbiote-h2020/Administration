package eu.h2020.symbiote.administration.services.platform;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage.DeploymentType;
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


    void returnPlatformConfiguration(HttpServletResponse response,
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
        DeploymentType deploymentType = configurationMessage.getDeploymentType();

        // Create .zip output stream
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"configuration.zip\"");
        response.addHeader("Content-Type", "application/zip");

        configureCloudConfigProperties(platformDetails, zipOutputStream, useBuiltInRapPlugin, deploymentType);
        configureNginx(zipOutputStream, platformDetails, level, deploymentType);
        configureComponentProperties(zipOutputStream, "Eureka", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
        configureComponentProperties(zipOutputStream, "RegistrationHandler", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
        configureComponentProperties(zipOutputStream, "ResourceAccessProxy", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
        configureComponentProperties(zipOutputStream, "Monitoring", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);

        if (level == PlatformConfigurationMessage.Level.L2) {
            configureComponentProperties(zipOutputStream, "FederationManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
            configureComponentProperties(zipOutputStream, "SubscriptionManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
            configureComponentProperties(zipOutputStream, "PlatformRegistry", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
            configureComponentProperties(zipOutputStream, "TrustManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
            configureComponentProperties(zipOutputStream, "BarteringAndTrading", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType);
        }

        configureAAMProperties(zipOutputStream, platformOwnerUsername, platformOwnerPassword, aamKeystoreName,
                aamKeystorePassword, aamPrivateKeyPassword, tokenValidity, deploymentType);
        configureCertProperties(zipOutputStream, platformId, platformOwnerUsernameInCore,
                platformOwnerPasswordInCore, aamKeystoreName, aamKeystorePassword, this.coreInterfaceAddress);

        zipOutputStream.close();
        response.getOutputStream().close();
    }

    private void configureCloudConfigProperties(OwnedService platformDetails,
                                                ZipOutputStream zipOutputStream,
                                                Boolean useBuiltInRapPlugin,
                                                DeploymentType deploymentType)
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
        switch (deploymentType) {
            case DOCKER:
                applicationProperties = applicationProperties.replaceFirst("(?m)^(spring.rabbitmq.host=).*$",
                        "spring.rabbitmq.host=symbiote-rabbitmq");
                applicationProperties = applicationProperties.replaceFirst("(?m)^(spring.data.mongodb.host=).*$",
                        "spring.data.mongodb.host=symbiote-mongo");
                applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.localaam.url=).*$",
                        "symbIoTe.localaam.url=http://symbiote-aam:8080");
                break;
            case MANUAL:
            default:
                applicationProperties = applicationProperties.replaceFirst("(?m)^(spring.rabbitmq.host=).*$",
                        "spring.rabbitmq.host=localhost");
                applicationProperties = applicationProperties.replaceFirst("(?m)^(spring.data.mongodb.host=).*$",
                        "spring.data.mongodb.host=localhost");
                applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.localaam.url=).*$",
                        "symbIoTe.localaam.url=http://localhost:8080");
        }

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
                        Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()));

        // RAP Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^.*(rap.enableSpecificPlugin=).*$",
                "ResourceAccessProxy.enableSpecificPlugin=" + useBuiltInRapPlugin);

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("CloudConfigProperties/application.properties"));
        InputStream stream = new ByteArrayInputStream(applicationProperties.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureNginx(ZipOutputStream zipOutputStream, OwnedService platformDetails,
                                PlatformConfigurationMessage.Level level,
                                DeploymentType deploymentType)
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

        Pattern p = Pattern.compile(":(\\d)+(/)?");   // the pattern to search for
        Matcher m = p.matcher(platformDetails.getPlatformInterworkingInterfaceAddress());
        String platformPort = "443";

        // if we find a match, get the group
        if (m.find()) {
            // we are only looking for the first occurrence
            platformPort = m.group(0).replaceAll(":", "").replaceAll("/", "");
            log.debug("The platform used the port: " + platformPort);
            nginxConf = nginxConf.replaceFirst("(?m)^.*(listen 443 ssl;).*$",
                    "        listen " + platformPort + " ssl;  ## HTTPS");
        }

        // Modify the nginx.conf file accordingly
        nginxConf = nginxConf.replaceAll("example_platform", platformDetails.getInstanceFriendlyName());
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https://\\{symbiote-core-hostname}/cloudCoreInterface).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.cloudCoreInterfaceAddress) + "/;");
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https://\\{symbiote-core-hostname}/coreInterface).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.coreInterfaceAddress) + "/;");

        if (deploymentType == DeploymentType.DOCKER) {
            nginxConf = nginxConf.replaceAll("/etc/nginx/ssl/fullchain.pem", "/certificates/fullchain.pem");
            nginxConf = nginxConf.replaceAll("/etc/nginx/ssl/privkey.pem", "/certificates/privkey.pem");
            nginxConf = nginxConf.replaceAll("localhost:8001", "symbiote-rh:8001");
            nginxConf = nginxConf.replaceAll("localhost:8103", "symbiote-rap:8103");
            nginxConf = nginxConf.replaceAll("localhost:8080", "symbiote-aam:8080");
            nginxConf = nginxConf.replaceAll("localhost:8202", "symbiote-fm:8202");
            nginxConf = nginxConf.replaceAll("localhost:8203", "symbiote-pr:8203");
            nginxConf = nginxConf.replaceAll("localhost:8128", "symbiote-sm:8128");
        }

        // packing the nginx.conf and nginx-prod.conf
        zipOutputStream.putNextEntry(new ZipEntry("nginx-prod.conf"));
        InputStream stream = new ByteArrayInputStream(nginxConf.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();

        zipOutputStream.putNextEntry(new ZipEntry("nginx.conf"));
        stream = new ByteArrayInputStream(nginxConf.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();


        // Create the nginx-ngrok.conf
        nginxConf = nginxConf.replaceFirst("(?m)^.*(ssl;  ## HTTPS).*$",
                "        #listen " + platformPort + " ssl;  ## HTTPS;");
        nginxConf = nginxConf.replaceAll("#listen 8102;", "listen 8102;");
        nginxConf = nginxConf.replaceAll("ssl_certificate", "#ssl_certificate");

        // packing the nginx-ngrok.conf
        zipOutputStream.putNextEntry(new ZipEntry("nginx-ngrok.conf"));
        stream = new ByteArrayInputStream(nginxConf.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureComponentProperties(ZipOutputStream zipOutputStream, String componentFolder,
                                              String platformOwnerUsername, String platformOwnerPassword,
                                              String keystorePassword, String platformId,
                                              OwnedService platformDetails, DeploymentType deploymentType) throws Exception {

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

        if (deploymentType == DeploymentType.DOCKER)
            propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                "spring.cloud.config.uri=" + Matcher.quoteReplacement("http://symbiote-cloudconfig:8888"));

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry(componentFolder + "/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureAAMProperties(ZipOutputStream zipOutputStream, String serviceOwnerUsername,
                                        String serviceOwnerPassword, String aamKeystoreName,
                                        String aamKeystorePassword, String aamPrivateKeyPassword,
                                        String tokenValidity, DeploymentType deploymentType)
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

        if (deploymentType == DeploymentType.DOCKER)
            propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                    "spring.cloud.config.uri=" + Matcher.quoteReplacement("http://symbiote-cloudconfig:8888"));

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
