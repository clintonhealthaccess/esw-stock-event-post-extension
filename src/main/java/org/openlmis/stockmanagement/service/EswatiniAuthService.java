package org.openlmis.stockmanagement.service;

import org.apache.commons.codec.binary.Base64;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.openlmis.stockmanagement.util.RequestHelper.createUri;

@Service
public class EswatiniAuthService {
    private static final String ACCESS_TOKEN = "access_token";

    @Value("${auth.server.authorizationUrl}")
    private String authorizationUrl;

    private RestOperations restTemplate = new RestTemplate();

    /**
     * Retrieves access token from the auth service.
     *
     * @return token.
     */
    @Cacheable("token")
    public String obtainAccessToken() {
        String plainCreds = "user-client" + ":" + "changeme";
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);

        HttpEntity<String> request = new HttpEntity<>(headers);

        RequestParameters params = RequestParameters
                .init()
                .set("grant_type", "password")
                .set("username", "chc_admin")
                .set("password", "password1");

        ResponseEntity<?> response = restTemplate.exchange(
                createUri(authorizationUrl, params), HttpMethod.POST, request, Object.class
        );

        return ((Map<String, String>) response.getBody()).get(ACCESS_TOKEN);
    }

    @CacheEvict(cacheNames = "token", allEntries = true)
    public void clearTokenCache() {
        // Intentionally blank
    }
}

