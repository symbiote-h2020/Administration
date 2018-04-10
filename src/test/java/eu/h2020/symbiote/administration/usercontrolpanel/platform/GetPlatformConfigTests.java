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
import java.lang.reflect.Field;
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

        String cloudCoreInterfaceAddress = this.coreInterfaceAddress
                .replace("8100", "8101")
                .replace("coreInterface", "cloudCoreInterface");

        // Checking application.properties of CloudConfigProperties
        String fileEntry = zipFiles.get("CloudConfigProperties/application.properties");
        assertTrue(fileEntry.contains("platform.id=" + platformId));
        assertTrue(fileEntry.contains("rabbit.host=localhost"));
        assertTrue(fileEntry.contains("rabbit.username=guest"));
        assertTrue(fileEntry.contains("rabbit.password=guest"));
        assertTrue(fileEntry.contains("symbIoTe.core.interface.url="
                + this.coreInterfaceAddress));
        assertTrue(fileEntry.contains("symbIoTe.core.cloud.interface.url="
                + cloudCoreInterfaceAddress));
        assertTrue(fileEntry.contains("symbIoTe.interworking.interface.url="
                + platformUrl + "/cloudCoreInterface"));
        assertTrue(fileEntry.contains("symbIoTe.localaam.url="
                + platformUrl + "/paam"));

        // Checking nginx.conf
        fileEntry = zipFiles.get("nginx.conf");
        assertTrue(fileEntry.contains("proxy_pass  " + coreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("proxy_pass  " + cloudCoreInterfaceAddress + "/;"));
        assertTrue(fileEntry.contains("listen " + platformPort + " ssl"));

        // Checking bootstrap.properties of Registration Handler
        fileEntry = zipFiles.get("RegistrationHandler/bootstrap.properties");
        assertTrue(fileEntry.contains("symbIoTe.component.username=" + username));
        assertTrue(fileEntry.contains("symbIoTe.component.password=" + password));
        assertTrue(fileEntry.contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));

        // Checking bootstrap.properties of RAP
        fileEntry = zipFiles.get("ResourceAccessProxy/bootstrap.properties");
        assertTrue(fileEntry.contains("symbIoTe.component.username=" + username));
        assertTrue(fileEntry.contains("symbIoTe.component.password=" + password));
        assertTrue(fileEntry.contains("symbIoTe.component.keystore.password=" + componentsKeystorePassword));


        // Checking bootstrap.properties of AAM
        fileEntry = zipFiles.get("AuthenticationAuthorizationManager/bootstrap.properties");
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
        assertTrue(fileEntry.contains("serviceId=" + platformId));
        assertTrue(fileEntry.contains("keyStoreFileName=" + aamKeystoreName));
        assertTrue(fileEntry.contains("keyStorePassword=" + aamKeystorePassword));
        assertTrue(fileEntry.contains("aamCertificateAlias=paam"));
        assertTrue(fileEntry.contains("rootCACertificateAlias=caam"));

    }
}