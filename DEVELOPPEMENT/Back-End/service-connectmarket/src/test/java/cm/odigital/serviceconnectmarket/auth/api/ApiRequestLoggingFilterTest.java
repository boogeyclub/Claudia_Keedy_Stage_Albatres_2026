package cm.odigital.serviceconnectmarket.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiRequestLoggingFilterTest {

    @Test
    void assignsACorrelationIdAndReturnsItToTheCaller() throws Exception {
        ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cacaomarketcm/api/auth/registration");
        request.setContextPath("/cacaomarketcm");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(202);
        });

        String requestId = response.getHeader(ApiRequestLoggingFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertTrue(requestId.matches("[A-Za-z0-9-]{8,64}"));
        assertEquals(202, response.getStatus());
        assertNull(MDC.get(ApiRequestLoggingFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void filtersApiRequestsBelowTheConfiguredServletContext() throws Exception {
        ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cacaomarketcm/api/health");
        request.setContextPath("/cacaomarketcm");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        assertNotNull(response.getHeader(ApiRequestLoggingFilter.REQUEST_ID_HEADER));
    }

    @Test
    void preservesASafeCallerSuppliedCorrelationId() throws Exception {
        ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cacaomarketcm/api/health");
        request.setContextPath("/cacaomarketcm");
        request.addHeader(ApiRequestLoggingFilter.REQUEST_ID_HEADER, "frontend-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        assertEquals("frontend-12345", response.getHeader(ApiRequestLoggingFilter.REQUEST_ID_HEADER));
    }
}
