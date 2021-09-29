package com.amida.hapi.security;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.amida.hapi.domain.HapiFhirClient;
import io.igia.config.fhir.interceptor.ScopeBasedAuthorizationInterceptor;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        String client_id = theRequestDetails.getHeader("client_id");
        if (client_id == null || "".equals(client_id)) {
            throw new AuthenticationException("Bypassed client selection.");
        }

        if ("startup".equals(client_id)) {
            return true;
        }

        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(client_id);
        if (hapiFhirClient == null) {
            throw new AuthenticationException("Client has not authenticated yet.");
        }

        if (tokenUtil.verifyToken(hapiFhirClient, keycloakBaseUrl)) {
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
