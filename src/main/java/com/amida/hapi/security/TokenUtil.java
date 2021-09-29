package com.amida.hapi.security;

import com.amida.hapi.domain.HapiFhirClient;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class TokenUtil {

    public static boolean verifyToken(HapiFhirClient hapiFhirClient) {
        WebTarget keycloakTarget = ClientBuilder.newClient().target(SecurityConfig.getKeycloakBaseUrl());
        return verifyToken(keycloakTarget, hapiFhirClient);
    }

    public static boolean verifyToken(WebTarget keycloakTarget, HapiFhirClient hapiFhirClient) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/userinfo");
        Invocation.Builder request = resource.request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hapiFhirClient.getToken().getAccessToken());

        Response response = request.get();
        System.out.println(response.getStatus());
        System.out.println(response.getStatusInfo().getReasonPhrase());

        return response.getStatus() == 200;
    }
}
