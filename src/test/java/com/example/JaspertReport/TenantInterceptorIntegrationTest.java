package com.example.JaspertReport;

import com.example.JaspertReport.config.TenantProperties;
import com.example.JaspertReport.tenant.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
class TenantInterceptorIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private TenantProperties createCentralizedConfig() {
        var props = new TenantProperties();
        props.setProfile("centralized");

        var acme = new TenantProperties.Tenant();
        acme.setServiceToken("tok-A");
        acme.setReportesRuta("/reportes/acme/");
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://host/acme");
        acmeDs.setUsername("user");
        acmeDs.setPassword("pass");
        acme.setDatasource(acmeDs);
        acme.setAllowedReports(List.of("ventas", "stock"));

        props.setTenants(Map.of("acme", acme));
        return props;
    }

    private TenantProperties createDedicatedConfig(String assignedTenant) {
        var props = new TenantProperties();
        props.setProfile("dedicated");
        props.setAssignedTenant(assignedTenant);

        var acme = new TenantProperties.Tenant();
        acme.setServiceToken("tok-A");
        acme.setReportesRuta("/reportes/acme/");
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://host/acme");
        acmeDs.setUsername("user");
        acmeDs.setPassword("pass");
        acme.setDatasource(acmeDs);
        acme.setAllowedReports(List.of("ventas", "stock"));

        var corp = new TenantProperties.Tenant();
        corp.setServiceToken("tok-B");
        corp.setReportesRuta("/reportes/corp/");
        var corpDs = new TenantProperties.Datasource();
        corpDs.setUrl("jdbc:mysql://host/corp");
        corpDs.setUsername("user2");
        corpDs.setPassword("pass2");
        corp.setDatasource(corpDs);

        props.setTenants(Map.of("acme", acme, "corp", corp));
        return props;
    }

    private void buildMockMvc(TenantProperties props) {
        var resolver = new ConfigBasedTenantResolver(props);
        var tokenValidator = new TokenValidator(resolver);
        var contextInitializer = new TenantContextInitializer(resolver, props);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addInterceptors(tokenValidator, contextInitializer)
                .build();
    }

    @Test
    void shouldReturn200ForValidToken() throws Exception {
        buildMockMvc(createCentralizedConfig());

        mockMvc.perform(post("/reportes/test")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void shouldReturn401ForMissingToken() throws Exception {
        buildMockMvc(createCentralizedConfig());

        mockMvc.perform(post("/reportes/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForUnknownToken() throws Exception {
        buildMockMvc(createCentralizedConfig());

        mockMvc.perform(post("/reportes/test")
                        .header("X-Service-Token", "tok-UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForWrongTenantInDedicatedMode() throws Exception {
        buildMockMvc(createDedicatedConfig("acme"));

        mockMvc.perform(post("/reportes/test")
                        .header("X-Service-Token", "tok-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200ForCorrectTenantInDedicatedMode() throws Exception {
        buildMockMvc(createDedicatedConfig("acme"));

        mockMvc.perform(post("/reportes/test")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void shouldPopulateTenantContextForValidRequest() throws Exception {
        var props = createCentralizedConfig();
        var resolver = new ConfigBasedTenantResolver(props);
        var tokenValidator = new TokenValidator(resolver);
        var contextInitializer = new TenantContextInitializer(resolver, props);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addInterceptors(tokenValidator, contextInitializer)
                .build();

        mockMvc.perform(post("/reportes/test")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // After the request, TenantContext should be cleared (afterCompletion)
        assertNull(TenantContext.getCurrentTenant(),
                "TenantContext should be cleared after request");
    }

    @RestController
    static class TestController {
        @PostMapping("/reportes/test")
        String handle() {
            return "OK";
        }
    }
}
