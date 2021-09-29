package com.amida.hapi.security;

import ca.uhn.fhir.context.FhirContext;
import com.amida.hapi.domain.HapiFhirClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

@Configuration
@ComponentScan(basePackageClasses = SecurityConfig.class)
public class SecurityConfig {

    @Value("${saraswati.keycloak.internal}")
    private String keycloakInternalUrl;

    @Value("${hspc.platform.authorization.tokenUrlPath}")
    private String tokenPath;

    private static FhirContext fhirContext;

    private static final Map<String, HapiFhirClient> inMemTokenStore = new HashMap<>();

    public static Map<String, HapiFhirClient> getInMemTokenStore() {
        return inMemTokenStore;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static FhirContext getFhirContext() {
        return fhirContext;
    }

    public static void setFhirContext(FhirContext client) {
        SecurityConfig.fhirContext = client;
    }

    @Bean
    protected OAuth2ProtectedResourceDetails resource() {
        ResourceOwnerPasswordResourceDetails resource;
        resource = new ResourceOwnerPasswordResourceDetails();

        List scopes = new ArrayList<String>(2);
        scopes.add("write");
        scopes.add("read");
        resource.setAccessTokenUri(keycloakInternalUrl + tokenPath);
        resource.setClientId("restapp");
        resource.setClientSecret("restapp");
        resource.setGrantType("password");
        resource.setScope(scopes);
        resource.setUsername("**USERNAME**");
        resource.setPassword("**PASSWORD**");
        return resource;
    }

    @Bean
    public OAuth2RestTemplate restTemplate() {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        return new OAuth2RestTemplate(resource(), new DefaultOAuth2ClientContext(atr));
    }

    @Bean
    public PrincipalExtractor hapiPrincipalExtractor() {
        return new HapiPrincipalExtractor();
    }

    @Bean
    public AuthoritiesExtractor hapiAuthoritiesExtractor() {
        return new HapiAuthoritiesExtractor();
    }

    public static String createState() {
        StringBuilder result = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int charactersLength = characters.length();
        for ( int i = 0; i < 12; i++ ) {
            result.append(characters.charAt(getRandomIndex(charactersLength)));
        }
        return result.toString();
    }

    private static int getRandomIndex(int length) {
        int index = (int) Math.round(Math.random() * length);
        if (index >= length) {
            index = length - 1;
        }
        return index;
    }

    public static String getAuthToken(WebTarget keycloakTarget) {
        Invocation.Builder request = keycloakTarget.path("/auth/realms/master/protocol/openid-connect/token").request(MediaType.APPLICATION_JSON_TYPE);
        Form form = new Form();
        form.param("client_id", "admin-cli");
        form.param("grant_type", "password");
        form.param("username", "admin");
        form.param("password", "admin");

        String json = request
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        String accessToken;
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            accessToken = jsonNode.get("access_token").asText();
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
        return accessToken;
    }
}
