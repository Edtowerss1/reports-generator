package com.example.JaspertReport.tenant;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidatorTest {

    @Mock
    private TenantResolver tenantResolver;

    private TokenValidator validator;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        validator = new TokenValidator(tenantResolver);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldRejectMissingHeader() throws Exception {
        boolean result = validator.preHandle(request, response, null);

        assertFalse(result, "Interceptor should block request when header is missing");
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void shouldRejectEmptyHeader() throws Exception {
        request.addHeader("X-Service-Token", "");

        boolean result = validator.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void shouldRejectBlankHeader() throws Exception {
        request.addHeader("X-Service-Token", "   ");

        boolean result = validator.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void shouldRejectUnknownToken() throws Exception {
        request.addHeader("X-Service-Token", "tok-X");
        when(tenantResolver.validate("tok-X")).thenReturn(false);

        boolean result = validator.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(tenantResolver).validate("tok-X");
    }

    @Test
    void shouldAllowValidToken() throws Exception {
        request.addHeader("X-Service-Token", "tok-A");
        when(tenantResolver.validate("tok-A")).thenReturn(true);

        boolean result = validator.preHandle(request, response, null);

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verify(tenantResolver).validate("tok-A");
    }
}
