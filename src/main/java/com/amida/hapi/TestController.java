package com.amida.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import com.amida.hapi.security.HapiAuthInterceptor;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.security.TokenBean;
import io.igia.config.fhir.interceptor.ScopeBasedAuthorizationInterceptor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.hl7.fhir.dstu2.model.Bundle;
import org.hl7.fhir.dstu2.model.Patient;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class TestController {

    private final FhirContext ctx;
    private final IGenericClient client;

    private final WebTarget keycloakTarget;

    private String redirect_url = "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
            "?response_type=code&client_id=igia-fhir-api-example&state=12345&scope=openid&redirect_uri=http://localhost:8080/hello";

    public TestController() {
        ctx = FhirContext.forDstu2Hl7Org();

        client = ctx.newRestfulGenericClient("http://hapi-fhir:9081/fhir");
        //client.registerInterceptor(getAuthenticationInterceptor());
        AdditionalRequestHeadersInterceptor headersInterceptor = new AdditionalRequestHeadersInterceptor();
        headersInterceptor.addHeaderValue("state", SecurityConfig.createState());
        client.registerInterceptor(headersInterceptor);
        client.getInterceptorService().registerInterceptor(new HapiAuthInterceptor());

        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target("http://keycloak:9080");
    }

    private ScopeBasedAuthorizationInterceptor getAuthenticationInterceptor() {
        return new ScopeBasedAuthorizationInterceptor(new InMemoryTokenStore(), new OAuth2RestTemplate(new AuthorizationCodeResourceDetails()));
    }

    @GetMapping(value = "/hello")
    public String helloWorld(@RequestParam String state, @RequestParam String session_state, @RequestParam String code) {
        return getToken(state, code).getAccessToken();
    }

    @GetMapping(value = "/hello2")
    public String helloWorld2() {
        return "Hello World2 " ;
    }

    @GetMapping(value = "/patient")
    public String getPatient() {
        System.out.println("Start rest call");
        //Patient pt = client.read().resource(Patient.class).withId("Patient/1").execute();
        Bundle results = client
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();
        System.out.println("End FHIR call");
        StringBuilder builder = new StringBuilder();

        for (Bundle.BundleEntryComponent result : results.getEntry()) {
            builder.append(result.getId()).append(" : ");
        }
        return builder.toString();
    }

    //@GetMapping(value = "/test")
    public TokenBean getToken(String state, String code) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/token")
                .queryParam("scope", "openid")
                .queryParam("state", state);

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", "smart-launch-context-app");
        form.param("client_secret", "smart-launch-context-app");
        form.param("code", code);
        form.param("redirect_uri", "http://localhost:9081/authorize");

        String json = resource.request(MediaType.APPLICATION_JSON_TYPE)
                                 .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        return new TokenBean(json);
    }

    @GetMapping(value = "/authorize", produces = MediaType.APPLICATION_JSON)
    public Object authorize(@RequestParam(required = false) String state,
                                  @RequestParam(required = false) String sessionState,
                                  @RequestParam(required = false) String code) {
        System.out.println("State = " + state);
        System.out.println("Session State = " + sessionState);
        System.out.println("Code = " + code);
        if (state == null) {
            return new ModelAndView("redirect:" + createURL("http://localhost:9081/authorize"));
        }
        TokenBean accessToken = getToken(state, code);
        String sofUrl = createSofUrl(state, code, accessToken);
        return accessToken;
    }

    private Response.ResponseBuilder createResponse(OutboundJaxrsResponse response1) {
        return Response.fromResponse(response1).status(301).location(getLocation());
    }

    private URI getLocation() {
        try {
            return new URI(redirect_url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    private String createSofUrl(String state, String code, TokenBean accessToken) {
        String token = createToken(state, code, accessToken);
        String value = "http://localhost:9080/#/patient?token=" +
                        token +
                        "&aud=http://localhost:9081/fhir" +
                        "&access_token=" + accessToken.getAccessToken();
        System.out.println("Full URL = " + value);
        return value;
    }

    private String createToken(String state, String code, TokenBean accessToken) {
        String value = "http://localhost:9081/auth/realms/igia/protocol/smart-openid-connect/smart-launch-context" +
                "?session_code=" + accessToken.getSession_state() +
                "&client_id=smart-launch-context-app" +
                "&tab_id=" + state +
                "&app-token={APP_TOKEN}";
        System.out.println("Token = " + value);
        return value;
    }

    private String createURL(String redirect_url) {
        return "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
                "?response_type=code&client_id=smart-launch-context-app&state=" + SecurityConfig.createState()
                + "&scope=openid&redirect_uri=" + redirect_url;
    }


}
