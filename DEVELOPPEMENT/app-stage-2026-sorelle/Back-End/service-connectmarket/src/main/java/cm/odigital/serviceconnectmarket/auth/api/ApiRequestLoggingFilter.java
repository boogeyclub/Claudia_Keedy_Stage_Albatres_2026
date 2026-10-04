package cm.odigital.serviceconnectmarket.auth.api;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestIdFor(request);
        long startedAt = System.nanoTime();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        LOGGER.info(
            "event=api.request.received method={} path={} clientAddress={} contentType={}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            safeContentType(request)
        );

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            // The response logger below records the final status. Do not log exception messages or request data,
            // because framework/database messages can contain values that should not be retained in operational logs.
            LOGGER.error("event=api.request.unhandled-failure exceptionType={}", exception.getClass().getName());
            throw exception;
        } finally {
            long elapsedMilliseconds = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            int status = response.getStatus();
            LOGGER.info(
                "event=api.request.completed method={} path={} status={} outcome={} durationMs={}",
                request.getMethod(),
                request.getRequestURI(),
                status,
                outcomeFor(status),
                elapsedMilliseconds
            );
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String requestIdFor(HttpServletRequest request) {
        String suppliedRequestId = request.getHeader(REQUEST_ID_HEADER);
        return suppliedRequestId != null && SAFE_REQUEST_ID.matcher(suppliedRequestId).matches()
            ? suppliedRequestId
            : UUID.randomUUID().toString();
    }

    private String safeContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType == null || contentType.isBlank() ? "[none]" : contentType;
    }

    private String outcomeFor(int status) {
        if (status >= 500) {
            return "server_error";
        }
        if (status >= 400) {
            return "client_error";
        }
        if (status >= 300) {
            return "redirect";
        }
        return "success";
    }
}
