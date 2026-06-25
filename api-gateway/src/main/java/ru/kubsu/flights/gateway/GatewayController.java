package ru.kubsu.flights.gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

@RestController
public class GatewayController {
    private final RestTemplate restTemplate;
    private final JwtVerifier jwtVerifier;
    private final String authServiceUrl;
    private final String searchServiceUrl;

    public GatewayController(
            RestTemplate restTemplate,
            JwtVerifier jwtVerifier,
            @Value("${app.auth-service-url}") String authServiceUrl,
            @Value("${app.search-service-url}") String searchServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.jwtVerifier = jwtVerifier;
        this.authServiceUrl = authServiceUrl;
        this.searchServiceUrl = searchServiceUrl;
    }

    @RequestMapping("/api/auth/**")
    ResponseEntity<String> auth(HttpServletRequest request) throws IOException {
        return forward(request, authServiceUrl, null);
    }

    @RequestMapping("/api/search/**")
    ResponseEntity<String> search(HttpServletRequest request) throws IOException {
        try {
            JwtVerifier.AuthenticatedUser user = jwtVerifier.verify(request.getHeader(HttpHeaders.AUTHORIZATION));
            return forward(request, searchServiceUrl, user.userId());
        } catch (RuntimeException exception) {
            return ResponseEntity.status(401).body("{\"error\":\"" + exception.getMessage() + "\"}");
        }
    }

    @RequestMapping("/")
    Map<String, String> root() {
        return Map.of("service", "api-gateway", "status", "ok");
    }

    private ResponseEntity<String> forward(HttpServletRequest request, String targetBaseUrl, UUID userId) throws IOException {
        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        HttpHeaders headers = copyHeaders(request);
        if (userId != null) {
            headers.set("X-User-Id", userId.toString());
        }
        String url = UriComponentsBuilder.fromHttpUrl(targetBaseUrl)
                .path(request.getRequestURI())
                .query(request.getQueryString())
                .build(true)
                .toUriString();
        try {
            return restTemplate.exchange(url, HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .headers(exception.getResponseHeaders() == null ? new HttpHeaders() : exception.getResponseHeaders())
                    .body(exception.getResponseBodyAsString());
        } catch (RestClientException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\":\"Upstream service is unavailable\"}");
        }
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (skipProxyHeader(name)) {
                continue;
            }
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }

    private boolean skipProxyHeader(String name) {
        return HttpHeaders.HOST.equalsIgnoreCase(name)
                || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)
                || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)
                || HttpHeaders.CONNECTION.equalsIgnoreCase(name)
                || "keep-alive".equalsIgnoreCase(name)
                || HttpHeaders.UPGRADE.equalsIgnoreCase(name)
                || HttpHeaders.PROXY_AUTHENTICATE.equalsIgnoreCase(name)
                || HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(name)
                || HttpHeaders.TRAILER.equalsIgnoreCase(name)
                || "te".equalsIgnoreCase(name);
    }
}
