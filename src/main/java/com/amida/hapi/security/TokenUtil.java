package com.amida.hapi.security;

import com.amida.hapi.domain.HapiFhirClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Configuration
public class TokenUtil {

    @Value("${hspc.platform.authorization.userinfoUrlPath}")
    private String userInfoPath;

    public boolean verifyToken(String keycloakBaseUrl, String accessToken) {
        WebTarget keycloakTarget = ClientBuilder.newClient().target(keycloakBaseUrl);
        return verifyToken(keycloakTarget, accessToken);
    }

    public boolean verifyToken(WebTarget keycloakTarget, HapiFhirClient hapiFhirClient) {
        return verifyToken(keycloakTarget, hapiFhirClient.getToken().getAccessToken());
    }

    public boolean verifyToken(WebTarget keycloakTarget, String accessToken) {
        WebTarget resource = keycloakTarget.path(userInfoPath);
        Invocation.Builder request = resource.request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        Response response = request.get();

        return response.getStatus() == 200;
    }
}
