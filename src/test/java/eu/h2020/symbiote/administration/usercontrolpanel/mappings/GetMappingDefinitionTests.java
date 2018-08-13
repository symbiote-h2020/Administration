package eu.h2020.symbiote.administration.usercontrolpanel.mappings;

import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.usercontrolpanel.UserControlPanelBaseTestClass;
import eu.h2020.symbiote.core.cci.InfoModelMappingResponse;
import eu.h2020.symbiote.core.internal.MappingListResponse;
import eu.h2020.symbiote.model.mim.OntologyMapping;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test class for use in testing MVC and form validation.
 */
public class GetMappingDefinitionTests extends UserControlPanelBaseTestClass {

    @Test
    public void couldNotFindInfoMappings() throws Exception {
        // Could not get Information mappings from Registry
        MappingListResponse response = sampleMappingListResponseFail();
        doReturn(sampleMappingListResponseFail()).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/get_mapping_definition")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingId", ontologyMappingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: " + response.getMessage()));
    }

    @Test
    public void registryError() throws Exception {
        // Registry returns error
        MappingListResponse response = sampleMappingListResponseFail();
        doReturn(response).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/get_mapping_definition")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingId", ontologyMappingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: " + response.getMessage()));
    }

    @Test
    public void registryTimeout() throws Exception {
        // Registry returns null
        doReturn(null).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/get_mapping_definition")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingId", ontologyMappingId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry unreachable!"));
    }

    @Test
    public void registryCommunicationException() throws Exception {
        // Registry throws CommunicationException
        doThrow(new CommunicationException("error")).when(rabbitManager).sendGetSingleMappingsRequest(any());

        mockMvc.perform(post("/administration/user/cpanel/get_mapping_definition")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingId", ontologyMappingId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("An error occurred: Registry threw communication exception: error"));
    }

    @Test
    public void success() throws Exception {
        // Get Mapping definition successfully
        doReturn(sampleMappingListResponseSuccess()).when(rabbitManager).sendGetSingleMappingsRequest(any());

        MvcResult mvcResult = mockMvc.perform(post("/administration/user/cpanel/get_mapping_definition")
                .with(authentication(sampleUserAuth(UserRole.SERVICE_OWNER)))
                .with(csrf().asHeader())
                .param("mappingId", ontologyMappingId))
                .andExpect(status().isOk())
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

        String fileEntry = zipFiles.get(ontologyMappingId + ".map");
        assertEquals(ontologyMappingDefinition, fileEntry);
    }
}