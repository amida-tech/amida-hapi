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

    public boolean verifyToken(HapiFhirClient hapiFhirClient, String keycloakBaseUrl) {
        WebTarget keycloakTarget = ClientBuilder.newClient().target(keycloakBaseUrl);
        return verifyToken(keycloakTarget, hapiFhirClient);
    }

    public boolean verifyToken(WebTarget keycloakTarget, HapiFhirClient hapiFhirClient) {
        WebTarget resource = keycloakTarget.path(userInfoPath);
        Invocation.Builder request = resource.request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hapiFhirClient.getToken().getAccessToken());

        Response response = request.get();

        return response.getStatus() == 200;
    }
}
