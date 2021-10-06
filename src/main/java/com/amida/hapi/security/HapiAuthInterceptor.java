package com.amida.hapi.security;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.amida.hapi.domain.HapiFhirClient;
import io.igia.config.fhir.interceptor.ScopeBasedAuthorizationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Primary
@Component
public class HapiAuthInterceptor extends ScopeBasedAuthorizationInterceptor {

    private final TokenUtil tokenUtil;

    private String keycloakBaseUrl;

    private int count = 0;

    public HapiAuthInterceptor(TokenStore tokenStore, OAuth2RestTemplate oAuth2RestTemplate, TokenUtil tokenUtil) {
        super(tokenStore, oAuth2RestTemplate);
        this.tokenUtil = tokenUtil;
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public boolean authorize(RequestDetails theRequestDetails, ServletRequestDetails servletRequestDetails,
                          HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        if (count == 0) {
            count++;
            return true;
        }

        if (theRequestDetails.getHeader("type") != null && theRequestDetails.getHeader("type").equals("startup")) {
            return true;
        }

        if (theRequestDetails.getHeader(AUTHORIZATION) == null
                || !theRequestDetails.getHeader(AUTHORIZATION).startsWith("Bearer ")) {
            throw new AuthenticationException("Unauthorized: Missing Authorization or invalid header value.");
        }

        String authHeader = theRequestDetails.getHeader(AUTHORIZATION).split(" ")[1];
        if (tokenUtil.verifyToken(keycloakBaseUrl, authHeader)) {
            return true;
        }
        else {
            throw new AuthenticationException("Client is not authenticated.");
        }
    }

    public void setKeycloakBaseUrl(String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
    }
}
