package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage.DeploymentType;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static eu.h2020.symbiote.administration.model.PlatformConfigurationMessage.DeploymentType.DOCKER;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for use in testing MVC and form validation.
 */
public class GetPlatformConfigTests extends UserControlPanelBaseTestClass {

    @Test
    public void getPlatformConfigUserNotOwningPlatform() throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        // User does not own the platform
        PlatformConfigurationMessage invalidPlatform = samplePlatformConfigurationMessage(PlatformConfigurationMessage.Level.L1,
                DeploymentType.MANUAL);
        Field platformIdField = invalidPlatform.getClass().getDeclaredField("platformId");
        platformIdField.setAccessible(true);
        platformIdField.set(invalidPlatform, "dummy");

        mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invalidPlatform)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }

    @Test
    public void getPlatformConfigL1ManualSuccess() throws Exception {
        testL1(DeploymentType.MANUAL);
    }

    @Test
    public void getPlatformConfigL1DockerSuccess() throws Exception {
        testL1(DOCKER);
    }

    @Test
    public void getPlatformConfigL2ManualSuccess() throws Exception {
        testL2(DeploymentType.MANUAL);
    }

    @Test
    public void getPlatformConfigL2DockerSuccess() throws Exception {
        testL2(DOCKER);
    }

    private void testL1(DeploymentType deploymentType) throws Exception {
        Map<String, String> zipFiles = testCommonFiles(coreInterfaceAddress,
                PlatformConfigurationMessage.Level.L1,
                deploymentType);

        assertNull(zipFiles.get("FederationManager/bootstrap.properties"));
        assertNull(zipFiles.get("SubscriptionManager/bootstrap.properties"));
        assertNull(zipFiles.get("PlatformRegistry/bootstrap.properties"));
        assertNull(zipFiles.get("TrustManager/bootstrap.properties"));
        assertNull(zipFiles.get("BarteringAndTrading/bootstrap.properties"));
        assertNull(zipFiles.get("SLAManager/bootstrap.properties"));
    }

    private void testL2(DeploymentType deploymentType) throws Exception {
        Map<String, String> zipFiles = testCommonFiles(coreInterfaceAddress,
                PlatformConfigurationMessage.Level.L2, deploymentType);

        testL2Files(zipFiles, deploymentType);
    }

    private Map<String, String> getZipFiles(MvcResult mvcResult) throws Exception {
        Map<String, String> zipFiles = new HashMap<>();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray()));

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zis, baos);
            zipFiles.put(fileName, baos.toString(StandardCharsets.UTF_8.name()));
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
        return zipFiles;
    }

    private Map<String, String> testCommonFiles(String coreInterfaceAddress, PlatformConfigurationMessage.Level level,
                                                DeploymentType deploymentType)
            throws Exception {
        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        // Successful Request
        MvcResult mvcResult = mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformConfigurationMessage(level, deploymentType))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"configuration.zip\""))
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

        Map<String, String> zipFiles = getZipFiles(mvcResult);

        String cloudCoreInterfaceAddress = this.coreInterfaceAddress
                .replace("8100", "8101")
                .replace("coreInterface", "cloudCoreInterface");

        // Checking application.properties of CloudConfigProperties
        String fileEntry = zipFiles.get("CloudConfigProperties/application.properties");
        assertTrue(fileEntry.contains("platform.id=" + platform1Id));
        assertTrue(fileEntry.contains("rabbit.host=${spring.rabbitmq.host}"));
        assertTrue(fileEntry.contains("rabbit.username=guest"));
        assertTrue(fileEntry.contains("rabbit.password=guest"));
        assertTrue(fileEntry.contains("aam.database.host=${spring.data.mongodb.host}"));
        assertTrue(fileEntry.contains("symbIoTe.core.interface.url=" + this.coreInterfaceAddress));
        assertTrue(fileEntry.contains("symbIoTe.core.cloud.interface.url=" + cloudCoreInterfaceAddress));
        assertTrue(fileEntry.contains("symbIoTe.interworking.interface.url=" + platform1Url));

        switch (deploymentType) {
            case DOCKER:
                assertTrue(fileEntry.contains("spring.rabbitmq.host=symbiote-rabbitmq"));
                assertTrue(fileEntry.contains("spring.data.mongodb.host=symbiote-mongo"));
                assertTrue(fileEntry.contains("symbIoTe.localaam.url=http://symbiote-aam:8080"));
                break;
            case MANUAL:
            default:
                assertTrue(fileEntry.contains("spring.rabbitmq.host=localhost"));
                assertTrue(fileEntry.contains("spring.data.mongodb.host=localhost"));
                assertTrue(fileEntry.contains("symbIoTe.localaam.url=http://localhost:8080"));

        }

        // Checking nginx-prod.conf
        fileEntry = zipFiles.get("nginx-prod.conf");
        String nginxConf = zipFiles.get("nginx.conf");
        assertEquals(fileEntry, nginxConf);

        assertTrue(fileEntry.contains("server_name  " + platform1Name + ";"));
        assertTrue(fileEntry.contains("proxy_pass  " + coreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("proxy_pass  " + cloudCoreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("listen " + platformPort + " ssl"));
        assertTrue(fileEntry.contains("#listen 8102"));
        testNginxL1ComponentPorts(fileEntry, deploymentType);

        if (deploymentType == DOCKER) {
            assertTrue(fileEntry.contains(" ssl_certificate     /certificates/fullchain.pem;"));
            assertTrue(fileEntry.contains(" ssl_certificate_key /certificates/privkey.pem;"));
        } else {
            assertTrue(fileEntry.contains(" ssl_certificate     /etc/nginx/ssl/fullchain.pem;"));
            assertTrue(fileEntry.contains(" ssl_certificate_key /etc/nginx/ssl/privkey.pem;"));
        }

        // Checking nginx-ngrok.conf
        fileEntry = zipFiles.get("nginx-ngrok.conf");
        assertTrue(fileEntry.contains("server_name  " + platform1Name + ";"));
        assertTrue(fileEntry.contains("proxy_pass  " + coreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("proxy_pass  " + cloudCoreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("#listen " + platformPort + " ssl"));
        assertTrue(fileEntry.contains(" listen 8102"));
        testNginxL1ComponentPorts(fileEntry, deploymentType);

        if (deploymentType == DOCKER) {
            assertTrue(fileEntry.contains("#ssl_certificate     /certificates/fullchain.pem;"));
            assertTrue(fileEntry.contains("#ssl_certificate_key /certificates/privkey.pem;"));
        } else {
            assertTrue(fileEntry.contains("#ssl_certificate     /etc/nginx/ssl/fullchain.pem;"));
            assertTrue(fileEntry.contains("#ssl_certificate_key /etc/nginx/ssl/privkey.pem;"));
        }

        // For Eureka test only the CloudConfigService uri
        testCloudConfigUri(zipFiles.get("Eureka/bootstrap.properties"), deploymentType);

        // Checking bootstrap.properties of components
        testComponentBootstrapProperties(zipFiles.get("RegistrationHandler/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("ResourceAccessProxy/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("Monitoring/bootstrap.properties"), deploymentType);

        fileEntry = zipFiles.get("AuthenticationAuthorizationManager/bootstrap.properties");
        if (deploymentType == DOCKER)
            assertTrue(fileEntry.contains("spring.cloud.config.uri=http://symbiote-cloudconfig:8888"));
        else
            assertTrue(fileEntry.contains("spring.cloud.config.uri=http://localhost:8888"));
        assertTrue(fileEntry.contains("aam.deployment.owner.username=" + username));
        assertTrue(fileEntry.contains("aam.deployment.owner.password=" + password));
        assertTrue(fileEntry.contains("aam.security.KEY_STORE_FILE_NAME=file:///#{systemProperties['user.dir']}/"
                + aamKeystoreName + ".p12"));
        assertTrue(fileEntry.contains("aam.security.ROOT_CA_CERTIFICATE_ALIAS=caam"));
        assertTrue(fileEntry.contains("aam.security.CERTIFICATE_ALIAS=paam"));
        assertTrue(fileEntry.contains("aam.security.KEY_STORE_PASSWORD=" + aamKeystorePassword));
        assertTrue(fileEntry.contains("aam.security.PV_KEY_PASSWORD=" + aamKeystorePassword));
        assertTrue(fileEntry.contains("aam.deployment.token.validityMillis=" + tokenValidity));

        // Checking cert.properties
        fileEntry = zipFiles.get("AuthenticationAuthorizationManager/cert.properties");
        assertTrue(fileEntry.contains("coreAAMAddress=" + coreInterfaceAddress));
        assertTrue(fileEntry.contains("serviceOwnerUsername=" + username));
        assertTrue(fileEntry.contains("serviceOwnerPassword=" + password));
        assertTrue(fileEntry.contains("serviceId=" + platform1Id));
        assertTrue(fileEntry.contains("keyStoreFileName=" + aamKeystoreName));
        assertTrue(fileEntry.contains("keyStorePassword=" + aamKeystorePassword));
        assertTrue(fileEntry.contains("aamCertificateAlias=paam"));
        assertTrue(fileEntry.contains("rootCACertificateAlias=caam"));

        return zipFiles;
    }

    public void testL2Files(Map<String, String> zipFiles, DeploymentType deploymentType) {

        // Checking bootstrap.properties of L2 components
        testComponentBootstrapProperties(zipFiles.get("FederationManager/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("SubscriptionManager/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("PlatformRegistry/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("TrustManager/bootstrap.properties"), deploymentType);
        testComponentBootstrapProperties(zipFiles.get("BarteringAndTrading/bootstrap.properties"), deploymentType);

        // Checking nginx.conf
        String fileEntry = zipFiles.get("nginx-prod.conf");
        testNginxL2ComponentPorts(fileEntry, deploymentType);

        fileEntry = zipFiles.get("nginx-ngrok.conf");
        testNginxL2ComponentPorts(fileEntry, deploymentType);
    }

    private void testNginxL1ComponentPorts(String file, DeploymentType deploymentType) {

        if (deploymentType == DOCKER) {
            assertTrue(file.contains(" proxy_pass http://symbiote-rh:8001/;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-rap:8103/notification;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-rap:8103;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-aam:8080/;"));
        } else {
            assertTrue(file.contains(" proxy_pass http://localhost:8001/;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8103/notification;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8103;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8080/;"));
        }
    }

    private void testNginxL2ComponentPorts(String file, DeploymentType deploymentType) {

        if (deploymentType == DOCKER) {
            assertTrue(file.contains(" proxy_pass http://symbiote-fm:8202;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-pr:8203;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-bt:8205/;"));
            assertTrue(file.contains(" proxy_pass http://symbiote-sm:8128;"));
        } else {
            assertTrue(file.contains(" proxy_pass http://localhost:8202;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8203;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8205/;"));
            assertTrue(file.contains(" proxy_pass http://localhost:8128;"));
        }
    }

    private void testComponentBootstrapProperties(String file, DeploymentType deploymentType) {
        testCloudConfigUri(file, deploymentType);
        assertTrue(file.contains("symbIoTe.component.username=" + username));
        assertTrue(file.contains("symbIoTe.component.password=" + password));
        assertTrue(file.contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));

    }

    private void testCloudConfigUri(String file, DeploymentType deploymentType) {
        if (deploymentType == DOCKER)
            assertTrue(file.contains("spring.cloud.config.uri=http://symbiote-cloudconfig:8888"));
        else
            assertTrue(file.contains("spring.cloud.config.uri=http://localhost:8888"));
    }
}