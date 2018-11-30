package eu.h2020.symbiote.administration.services.platform;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage.DeploymentType;
import eu.h2020.symbiote.administration.services.git.IGitService;
import eu.h2020.symbiote.security.communication.payloads.OwnedService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static eu.h2020.symbiote.administration.model.PlatformConfigurationMessage.*;

@Service
public class PlatformConfigurer {

    private static Log log = LogFactory.getLog(PlatformConfigurer.class);
    private static final String FILE_PATH_PREFIX = "file://";

    private ResourceLoader resourceLoader;
    private IGitService gitService;
    private String coreInterfaceAddress;
    private String paamValidityMillis;
    private String cloudCoreInterfaceAddress;
    private String cloudConfigGitPath;
    private String enablerConfigGitPath;

    @Autowired
    public PlatformConfigurer(ResourceLoader resourceLoader,
                              IGitService gitService,
                              @Value("${aam.environment.coreInterfaceAddress}") String coreInterfaceAddress,
                              @Value("${paam.deployment.token.validityMillis}") String paamValidityMillis,
                              @Value("${symbiote.core.cloudconfig.git.path}") String cloudConfigGitPath,
                              @Value("${symbiote.core.enablerconfig.git.path}") String enablerConfigGitPath) {

        this.resourceLoader = resourceLoader;

        this.gitService = gitService;

        Assert.notNull(coreInterfaceAddress,"coreInterfaceAddress can not be null!");
        this.coreInterfaceAddress = coreInterfaceAddress;

        Assert.notNull(paamValidityMillis,"paamValidityMillis can not be null!");
        this.paamValidityMillis = paamValidityMillis;

        this.cloudCoreInterfaceAddress = this.coreInterfaceAddress
                .replace("8100", "8101")
                .replace("coreInterface", "cloudCoreInterface");

        Assert.notNull(cloudConfigGitPath,"cloudConfigGitPath can not be null!");
        this.cloudConfigGitPath = cloudConfigGitPath;

        Assert.notNull(enablerConfigGitPath,"enablerConfigGitPath can not be null!");
        this.enablerConfigGitPath = enablerConfigGitPath;
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
        Level level = configurationMessage.getLevel();
        DeploymentType deploymentType = configurationMessage.getDeploymentType();

        // Create .zip output stream
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"configuration.zip\"");
        response.addHeader("Content-Type", "application/zip");

        configureCloudConfigProperties(platformDetails, zipOutputStream, useBuiltInRapPlugin, deploymentType, level);
        configureNginx(zipOutputStream, platformDetails, level, deploymentType);
        configureComponentProperties(zipOutputStream, "Eureka", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
        configureComponentProperties(zipOutputStream, "RegistrationHandler", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
        configureComponentProperties(zipOutputStream, "ResourceAccessProxy", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
        configureComponentProperties(zipOutputStream, "Monitoring", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);

        if (level == Level.L2) {
            configureComponentProperties(zipOutputStream, "FederationManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureComponentProperties(zipOutputStream, "SubscriptionManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureComponentProperties(zipOutputStream, "PlatformRegistry", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureComponentProperties(zipOutputStream, "TrustManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureComponentProperties(zipOutputStream, "BarteringAndTrading", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
        } else if (level == Level.ENABLER) {
            configureComponentProperties(zipOutputStream, "EnablerResourceManager", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureComponentProperties(zipOutputStream, "EnablerPlatformProxy", platformOwnerUsername,
                    platformOwnerPassword, componentKeystorePassword, platformId, platformDetails, deploymentType, level);
            configureEnablerLogicExample(zipOutputStream, platformOwnerUsername, platformOwnerPassword, componentKeystorePassword, deploymentType);
        }

        if (level != Level.ENABLER) {
            configureRAPPluginStarter(zipOutputStream, deploymentType);
        }

        configureAAMProperties(zipOutputStream, platformOwnerUsername, platformOwnerPassword, aamKeystoreName,
                aamKeystorePassword, aamPrivateKeyPassword, tokenValidity, level, deploymentType);
        configureCertProperties(zipOutputStream, platformId, platformOwnerUsernameInCore,
                platformOwnerPasswordInCore, aamKeystoreName, aamKeystorePassword, this.coreInterfaceAddress);

        zipOutputStream.close();
        response.getOutputStream().close();
    }

    private void configureCloudConfigProperties(OwnedService platformDetails,
                                                ZipOutputStream zipOutputStream,
                                                Boolean useBuiltInRapPlugin,
                                                DeploymentType deploymentType,
                                                Level level)
            throws Exception {
        String propertiesFolderName = "";
        String propertiesGitPath = "";

        try {
            if (level == Level.ENABLER) {
                propertiesFolderName = "EnablerConfigProperties";
                propertiesGitPath = this.enablerConfigGitPath;
                gitService.pullRepo(this.enablerConfigGitPath);
            } else {
                propertiesFolderName = "CloudConfigProperties";
                propertiesGitPath = this.cloudConfigGitPath;
                gitService.pullRepo(this.cloudConfigGitPath);
            }
        } catch (Exception e) {
            log.warn("Thrown exception during pulling propertiesFolderName. The config might not be up to date", e);
        }

        Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(FILE_PATH_PREFIX + propertiesGitPath + "/*");

        for (Resource resource : resources) {
            String resourceName = resource.getFilename();
            String filePath = propertiesGitPath + "/" + resourceName;

            if (!resourceName.endsWith("application.properties")) {
                zipFile(zipOutputStream, new File(filePath), propertiesFolderName);
            } else {

                // Loading application.properties
                InputStream propertiesResourceAsStream = resource.getInputStream();
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
                        "rap.enableSpecificPlugin=" + useBuiltInRapPlugin);

                // Packing files
                zipOutputStream.putNextEntry(new ZipEntry(propertiesFolderName + "/" + resourceName));
                InputStream inputStream = new ByteArrayInputStream(applicationProperties.getBytes(StandardCharsets.UTF_8.name()));
                IOUtils.copy(inputStream, zipOutputStream);
                inputStream.close();
            }



        }
    }


    private void configureNginx(ZipOutputStream zipOutputStream, OwnedService platformDetails,
                                Level level,
                                DeploymentType deploymentType)
            throws Exception {

        StringBuilder sb = new StringBuilder("classpath:files/nginx_l");

        if (level == Level.L1 || level == Level.ENABLER)
            sb.append("1.conf");
        else if (level == Level.L2)
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
            nginxConf = nginxConf.replaceAll("localhost:8205", "symbiote-bt:8205");
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
                                              OwnedService platformDetails, DeploymentType deploymentType,
                                              Level level) throws Exception {

        // Loading component properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/" + componentFolder + "/bootstrap.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

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

        if (deploymentType == DeploymentType.DOCKER) {
            if (level == Level.ENABLER) {
                propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                        "spring.cloud.config.uri=" + Matcher.quoteReplacement("http://symbiote-enablerconfig:8888"));
            } else {
                propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                        "spring.cloud.config.uri=" + Matcher.quoteReplacement("http://symbiote-cloudconfig:8888"));
            }
        }

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry(componentFolder + "/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureAAMProperties(ZipOutputStream zipOutputStream, String serviceOwnerUsername,
                                        String serviceOwnerPassword, String aamKeystoreName,
                                        String aamKeystorePassword, String aamPrivateKeyPassword,
                                        String tokenValidity, Level level,
                                        DeploymentType deploymentType)
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

        if (deploymentType == DeploymentType.DOCKER) {
            String configServer = level == Level.ENABLER ? "http://symbiote-enablerconfig:8888" : "http://symbiote-cloudconfig:8888";
            propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                    "spring.cloud.config.uri=" + Matcher.quoteReplacement(configServer));
        }

        // Packing files
        zipOutputStream.putNextEntry(new ZipEntry("AuthenticationAuthorizationManager/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();


        // Loading CloudConfigProperties/AuthenticationAuthorizationManager.properties
        String propertiesName;
        String propertiesGitPath;

        if (level == Level.ENABLER) {
            propertiesName = "EnablerConfigProperties";
            propertiesGitPath =  this.enablerConfigGitPath;
        } else {
            propertiesName = "CloudConfigProperties";
            propertiesGitPath =  this.cloudConfigGitPath;
        }

        InputStream aamPropertiesAsStream = resourceLoader
                .getResource("file://" + propertiesGitPath + "/AuthenticationAuthorizationManager.properties").getInputStream();
        propertiesAsStream = new BufferedReader(new InputStreamReader(aamPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.token.validityMillis).*$",
                "aam.deployment.token.validityMillis=" + Matcher.quoteReplacement(tokenValidity));

        // Packing files
        zipOutputStream.putNextEntry(new ZipEntry(propertiesName + "/AuthenticationAuthorizationManager.properties"));
        stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
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

    private void configureEnablerLogicExample(ZipOutputStream zipOutputStream, String platformOwnerUsername,
                                              String platformOwnerPassword, String keystorePassword,
                                              DeploymentType deploymentType) throws Exception {

        // Loading component properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/EnablerLogicExample/bootstrap.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.username=).*$",
                "symbIoTe.component.username=" + Matcher.quoteReplacement(platformOwnerUsername));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.password=).*$",
                "symbIoTe.component.password=" + Matcher.quoteReplacement(platformOwnerPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(symbIoTe.component.keystore.password=).*$",
                "symbIoTe.component.keystore.password=" + Matcher.quoteReplacement(keystorePassword));


        if (deploymentType == DeploymentType.DOCKER) {
                propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(spring.cloud.config.uri=).*$",
                        "spring.cloud.config.uri=" + Matcher.quoteReplacement("http://symbiote-enablerconfig:8888"));
            propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(enablerLogic.registrationHandlerUrl=).*$",
                    "enablerLogic.registrationHandlerUrl=" + Matcher.quoteReplacement("http://symbiote-rh:8001"));
        }

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("EnablerLogicExample/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }

    private void configureRAPPluginStarter(ZipOutputStream zipOutputStream, DeploymentType deploymentType) throws Exception {

        // Loading component properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/RAPPluginStarter/application.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        if (deploymentType == DeploymentType.DOCKER) {
            propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(rabbit.host=).*$",
                    "rabbit.host=symbiote-rabbitmq");
        }

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("RAPPluginStarter/application.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }

    private void zipFile(ZipOutputStream zipOutputStream, File file, String parentFolder)
            throws Exception {
        String resourceName = file.getName();

        // Skip the AuthenticationAuthorizationManager entry because it will be set in the configureAAMProperties()
        if (resourceName.equals("AuthenticationAuthorizationManager.properties"))
            return;

        String filePath = file.getAbsolutePath();
        String fileNameInZip = parentFolder + "/" + resourceName;

        if (file.isDirectory()) {
            if (fileNameInZip.endsWith("/")) {
                zipOutputStream.putNextEntry(new ZipEntry(fileNameInZip));
                zipOutputStream.closeEntry();
            } else {
                zipOutputStream.putNextEntry(new ZipEntry(fileNameInZip + "/"));
                zipOutputStream.closeEntry();
            }

            File folder = new File(filePath);
            File[] children = folder.listFiles();
            for (File childFile : children) {
                zipFile(zipOutputStream, childFile, fileNameInZip);
            }

            return;
        }

        zipOutputStream.putNextEntry(new ZipEntry(fileNameInZip));
        InputStream inputStream = resourceLoader.getResource(FILE_PATH_PREFIX + filePath).getInputStream();
        IOUtils.copy(inputStream, zipOutputStream);
        inputStream.close();

    }
}
