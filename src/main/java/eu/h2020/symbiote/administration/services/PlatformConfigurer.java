package eu.h2020.symbiote.administration.services;

import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.security.communication.payloads.OwnedPlatformDetails;

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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PlatformConfigurer {

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

        this.cloudCoreInterfaceAddress = this.coreInterfaceAddress.replace("8100/coreInterface", "8101/cloudCoreInterface");
    }


    public void returnPlatformConfiguration(HttpServletResponse response,
                                         CoreUser user,
                                         OwnedPlatformDetails platformDetails,
                                         PlatformConfigurationMessage configurationMessage) throws Exception {

        String platformId = configurationMessage.getPlatformId();
        String platformOwnerUsername = configurationMessage.getPlatformOwnerUsername();
        String platformOwnerPassword = configurationMessage.getPlatformOwnerPassword();
        String platformOwnerUsernameInCore = user.getUsername();
        String platformOwnerPasswordInCore = user.getPassword();
        String componentKeystorePassword = configurationMessage.getComponentsKeystorePassword().isEmpty() ?
                "pass" : configurationMessage.getComponentsKeystorePassword();
        String aamKeystoreName = configurationMessage.getAamKeystoreName().isEmpty() ?
                "paam-keystore-" + platformDetails.getPlatformInstanceFriendlyName() :
                configurationMessage.getAamKeystoreName();
        String aamKeystorePassword = configurationMessage.getAamKeystorePassword().isEmpty() ?
                "pass" : configurationMessage.getAamKeystorePassword();
        String aamPrivateKeyPassword = configurationMessage.getAamPrivateKeyPassword().isEmpty() ?
                "pass" : configurationMessage.getAamPrivateKeyPassword();
        String sslKeystore = configurationMessage.getSslKeystore();
        String sslKeystorePassword = configurationMessage.getSslKeystorePassword();
        String sslKeyPassword = configurationMessage.getSslKeyPassword();
        Boolean useBuiltInRapPlugin = configurationMessage.getUseBuiltInRapPlugin();

        // Create .zip output stream
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"configuration.zip\"");
        response.addHeader("Content-Type", "application/zip");

        configureCloudConfigProperties(platformDetails, zipOutputStream, useBuiltInRapPlugin);
        configureNginx(zipOutputStream);
        configureComponentProperties(zipOutputStream, "registrationHandler", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword);
        configureComponentProperties(zipOutputStream, "rap", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword);
        configureComponentProperties(zipOutputStream, "monitoring", platformOwnerUsername,
                platformOwnerPassword, componentKeystorePassword);

        configureAAMProperties(zipOutputStream, platformOwnerUsername, platformOwnerPassword, aamKeystoreName,
                aamKeystorePassword, aamPrivateKeyPassword, sslKeystore, sslKeystorePassword, sslKeyPassword);
        configurePlatformAAMCertificateKeyStoreFactory(zipOutputStream, platformId, platformOwnerUsernameInCore,
                platformOwnerPasswordInCore, aamKeystoreName, aamKeystorePassword, this.coreInterfaceAddress);

        zipOutputStream.close();
        response.getOutputStream().close();
    }

    private void configureCloudConfigProperties(OwnedPlatformDetails platformDetails, ZipOutputStream zipOutputStream,
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
                "platform.id=" + Matcher.quoteReplacement(platformDetails.getPlatformInstanceId()));

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
                        "/cloudCoreInterface/v1");
        applicationProperties = applicationProperties.replaceFirst("(?m)^(symbIoTe.localaam.url=).*$",
                "symbIoTe.localaam.url=" + Matcher.quoteReplacement(platformDetails.getPlatformInterworkingInterfaceAddress()) +
                        "/paam");

        // RAP Configuration
        applicationProperties = applicationProperties.replaceFirst("(?m)^.*(rap.enableSpecificPlugin=).*$",
                "rap.enableSpecificPlugin=" + useBuiltInRapPlugin.booleanValue());

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("CloudConfigProperties/application.properties"));
        InputStream stream = new ByteArrayInputStream(applicationProperties.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureNginx(ZipOutputStream zipOutputStream) throws Exception {

        // Loading nginx.conf
        InputStream nginxConfAsStream = resourceLoader
                .getResource("classpath:files/nginx.conf").getInputStream();
        String nginxConf = new BufferedReader(new InputStreamReader(nginxConfAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https:\\/\\/\\{symbiote-core-hostname\\}:8101).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.cloudCoreInterfaceAddress) + "/;");
        nginxConf = nginxConf.replaceFirst("(?m)^.*(https:\\/\\/\\{symbiote-core-hostname\\}:8100).*$",
                "          proxy_pass  " + Matcher.quoteReplacement(this.coreInterfaceAddress) + "/;");

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("nginx.conf"));
        InputStream stream = new ByteArrayInputStream(nginxConf.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureComponentProperties(ZipOutputStream zipOutputStream, String componentFolder,
                                              String platformOwnerUsername, String platformOwnerPassword,
                                              String keystorePassword) throws Exception {

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

        //packing files
        zipOutputStream.putNextEntry(new ZipEntry(componentFolder + "/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configureAAMProperties(ZipOutputStream zipOutputStream, String platformOwnerUsername,
                                        String platformOwnerPassword, String aamKeystoreName,
                                        String aamKeystorePassword, String aamPrivateKeyPassword,
                                        String sslKeystore, String sslKeystorePassword, String sslKeyPassword)
            throws Exception {

        // Loading AAM bootstrap.properties
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/aam/bootstrap.properties").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.owner.username=).*$",
                "aam.deployment.owner.username=" + Matcher.quoteReplacement(platformOwnerUsername));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.owner.password=).*$",
                "aam.deployment.owner.password=" + Matcher.quoteReplacement(platformOwnerPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.KEY_STORE_FILE_NAME=).*$",
                "aam.security.KEY_STORE_FILE_NAME=" + Matcher.quoteReplacement(aamKeystoreName) + ".p12");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.ROOT_CA_CERTIFICATE_ALIAS=).*$",
                "aam.security.ROOT_CA_CERTIFICATE_ALIAS=caam");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.CERTIFICATE_ALIAS=).*$",
                "aam.security.CERTIFICATE_ALIAS=paam");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.KEY_STORE_PASSWORD=).*$",
                "aam.security.KEY_STORE_PASSWORD=" + Matcher.quoteReplacement(aamKeystorePassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.security.PV_KEY_PASSWORD=).*$",
                "aam.security.PV_KEY_PASSWORD=" + Matcher.quoteReplacement(aamPrivateKeyPassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(aam.deployment.token.validityMillis=).*$",
                "aam.deployment.token.validityMillis=" + Matcher.quoteReplacement(paamValidityMillis));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(server.ssl.key-store=).*$",
                "server.ssl.key-store=classpath:" + Matcher.quoteReplacement(sslKeystore));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(server.ssl.key-store-password=).*$",
                "server.ssl.key-store-password=" + Matcher.quoteReplacement(sslKeystorePassword));
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(server.ssl.key-password=).*$",
                "server.ssl.key-password=" + Matcher.quoteReplacement(sslKeyPassword));


        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("aam/bootstrap.properties"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }


    private void configurePlatformAAMCertificateKeyStoreFactory(ZipOutputStream zipOutputStream, String platformId,
                                                                String platformOwnerUsernameInCore,
                                                                String platformOwnerPasswordInCore,
                                                                String aamKeystoreName, String aamKeystorePassword,
                                                                String coreAAMAddress)
            throws Exception {

        // Loading PlatformAAMCertificateKeyStoreFactory
        InputStream bootstrapPropertiesAsStream = resourceLoader
                .getResource("classpath:files/symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java").getInputStream();
        String propertiesAsStream = new BufferedReader(new InputStreamReader(bootstrapPropertiesAsStream))
                .lines().collect(Collectors.joining("\n"));

        // Modify the nginx.conf file accordingly
        // AMQP Configuration
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String coreAAMAddress = ).*$",
                "        String coreAAMAddress = \"" + Matcher.quoteReplacement(coreAAMAddress) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String platformOwnerUsername =).*$",
                "        String platformOwnerUsername = \"" + Matcher.quoteReplacement(platformOwnerUsernameInCore) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String platformOwnerPassword = ).*$",
                "        String platformOwnerPassword = \"" + Matcher.quoteReplacement(platformOwnerPasswordInCore) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String platformId = ).*$",
                "        String platformId = \"" + Matcher.quoteReplacement(platformId) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String keyStoreFileName =).*$",
                "        String keyStoreFileName = \"" + Matcher.quoteReplacement(aamKeystoreName) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String keyStorePassword = ).*$",
                "        String keyStorePassword = \"" + Matcher.quoteReplacement(aamKeystorePassword) + "\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String aamCertificateAlias = ).*$",
                "        String aamCertificateAlias = \"paam\";");
        propertiesAsStream = propertiesAsStream.replaceFirst("(?m)^.*(String rootCACertificateAlias =).*$",
                "        String rootCACertificateAlias = \"caam\";");


        //packing files
        zipOutputStream.putNextEntry(new ZipEntry("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java"));
        InputStream stream = new ByteArrayInputStream(propertiesAsStream.getBytes(StandardCharsets.UTF_8.name()));
        IOUtils.copy(stream, zipOutputStream);
        stream.close();
    }
}
