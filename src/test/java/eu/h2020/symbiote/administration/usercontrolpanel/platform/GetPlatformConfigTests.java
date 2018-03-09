package eu.h2020.symbiote.administration.usercontrolpanel.platform;

import eu.h2020.symbiote.administration.model.PlatformConfigurationMessage;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertTrue;
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
        PlatformConfigurationMessage invalidPlatform = samplePlatformConfigurationMessage();
        invalidPlatform.setPlatformId("dummy");

        mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(invalidPlatform)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You do not own the platform with id dummy"));
    }

    @Test
    public void getPlatformConfigSuccess() throws Exception {

        doReturn(sampleOwnedServiceDetails()).when(rabbitManager)
                .sendOwnedServiceDetailsRequest(any());

        // Successful Request
        MvcResult mvcResult = mockMvc.perform(post("/administration/user/cpanel/get_platform_config")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content(serialize(samplePlatformConfigurationMessage())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"configuration.zip\""))
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn();

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

        String cloudCoreInterfaceAddress = this.coreInterfaceAddress.replace("8100/coreInterface", "8101/cloudCoreInterface");

        // Checking application.properties of CloudConfigProperties
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.host=localhost"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.username=guest"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("rabbit.password=guest"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.core.interface.url="
                + this.coreInterfaceAddress));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.core.cloud.interface.url="
                + cloudCoreInterfaceAddress));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.interworking.interface.url="
                + platformUrl + "/cloudCoreInterface/v1"));
        assertTrue(zipFiles.get("CloudConfigProperties/application.properties").contains("symbIoTe.localaam.url="
                + platformUrl + "/paam"));

        // Checking nginx.conf
        assertTrue(zipFiles.get("nginx.conf").contains("proxy_pass  " + coreInterfaceAddress + "/;"));
        assertTrue(zipFiles.get("nginx.conf").contains("proxy_pass  " + cloudCoreInterfaceAddress + "/;"));
        assertTrue(zipFiles.get("nginx.conf").contains("listen " + platformPort + " ssl"));

        // Checking bootstrap.properties of Registration Handler
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("registrationHandler/bootstrap.properties")
                .contains("symbIoTe.interworking.interface.url=" + platformUrl));

        // Checking bootstrap.properties of RAP
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("rap/bootstrap.properties")
                .contains("symbiote.notification.url=" + cloudCoreInterfaceAddress + "/accessNotifications"));

        // Checking bootstrap.properties of Monitoring
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.username=" + username));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.password=" + password));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.core.interface.url=" + coreInterfaceAddress));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.localaam.url=" + platformUrl + "/paam"));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("platform.id=" + platformId));
        assertTrue(zipFiles.get("monitoring/bootstrap.properties")
                .contains("symbIoTe.interworking.interface.url=" + platformUrl));

        // Checking bootstrap.properties of AAM
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.owner.username=" + username));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.owner.password=" + password));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.KEY_STORE_FILE_NAME=" + aamKeystoreName + ".p12"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.ROOT_CA_CERTIFICATE_ALIAS=caam"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.CERTIFICATE_ALIAS=paam"));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.KEY_STORE_PASSWORD=" + aamKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.security.PV_KEY_PASSWORD=" + aamKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("aam.deployment.token.validityMillis=" + tokenValidity));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-store=classpath:" + sslKeystore));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-store-password=" + sslKeystorePassword));
        assertTrue(zipFiles.get("aam/bootstrap.properties")
                .contains("server.ssl.key-password=" + sslKeyPassword));

        // Checking PlatformAAMCertificateKeyStoreFactory
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String coreAAMAddress = \"" + coreInterfaceAddress + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformOwnerUsername = \"" + username + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformOwnerPassword = \"" + password + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String platformId = \"" + platformId + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String keyStoreFileName = \"" + aamKeystoreName + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String keyStorePassword = \"" + aamKeystorePassword + "\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String aamCertificateAlias = \"paam\";"));
        assertTrue(zipFiles.get("symbioteSecurity/PlatformAAMCertificateKeyStoreFactory.java")
                .contains("        String rootCACertificateAlias = \"caam\";"));

    }
}